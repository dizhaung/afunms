package com.afunms.polling.snmp.db;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import com.afunms.alarm.model.AlarmIndicatorsNode;
import com.afunms.alarm.util.AlarmConstant;
import com.afunms.alarm.util.AlarmIndicatorsUtil;
import com.afunms.application.dao.DBDao;
import com.afunms.application.dao.SybspaceconfigDao;
import com.afunms.application.model.DBVo;
import com.afunms.application.model.SybaseVO;
import com.afunms.application.model.Sybspaceconfig;
import com.afunms.application.model.TablesVO;
import com.afunms.application.util.IpTranslation;
import com.afunms.common.util.CheckEventUtil;
import com.afunms.common.util.EncryptUtil;
import com.afunms.common.util.ShareData;
import com.afunms.common.util.SysLogger;
import com.afunms.indicators.dao.NodeGatherIndicatorsDao;
import com.afunms.indicators.model.NodeDTO;
import com.afunms.indicators.model.NodeGatherIndicators;
import com.afunms.indicators.util.NodeUtil;
import com.afunms.polling.PollingEngine;
import com.afunms.polling.node.DBNode;
import com.afunms.system.util.TimeGratherConfigUtil;

public class SybaseDataCollector {

	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");

	public Hashtable collect_Data(NodeGatherIndicators nodeGatherIndicator) {
		DBDao dbdao = null;
		Hashtable returndata = new Hashtable();
		List dbmonitorlists = new ArrayList();
		NodeGatherIndicatorsDao indicatorsdao = new NodeGatherIndicatorsDao();
		List<NodeGatherIndicators> monitorItemList = new ArrayList<NodeGatherIndicators>();
		dbmonitorlists = ShareData.getDBList();
		try {
			DBVo dbmonitorlist = new DBVo();
			if (dbmonitorlists != null && dbmonitorlists.size() > 0) {
				for (int i = 0; i < dbmonitorlists.size(); i++) {
					DBVo vo = (DBVo) dbmonitorlists.get(i);
					if (vo.getId() == Integer.parseInt(nodeGatherIndicator.getNodeid())) {
						dbmonitorlist = vo;
						break;
					}
				}
			}
			// δ����
			if (dbmonitorlist.getManaged() == 0) {
				// ���δ�����������ɼ�����ϢΪ��
				return returndata;
			}
			try {
				// ��ȡ�����õ�DB2���б�����ָ��
				monitorItemList = indicatorsdao.getByInterval("5", "m", 1, "db", "sybase");
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				indicatorsdao.close();
			}

			if (monitorItemList == null) {
				monitorItemList = new ArrayList<NodeGatherIndicators>();
			}
			Hashtable gatherHash = new Hashtable();
			for (int i = 0; i < monitorItemList.size(); i++) {
				NodeGatherIndicators nodeGatherIndicators = monitorItemList.get(i);
				if (nodeGatherIndicators.getNodeid().equals(nodeGatherIndicator.getNodeid())) {
					gatherHash.put(nodeGatherIndicators.getName(), nodeGatherIndicators);
				}
			}
			DBNode dbnode = (DBNode) PollingEngine.getInstance().getDbByID(dbmonitorlist.getId());
			// �ж��豸�Ƿ��ڲɼ�ʱ����� 0:���ڲɼ�ʱ�����,���˳�;1:��ʱ�����,���вɼ�;2:�����ڲɼ�ʱ�������,��ȫ��ɼ�
			TimeGratherConfigUtil timeconfig = new TimeGratherConfigUtil();
			int result = 0;
			result = timeconfig.isBetween(dbnode.getId() + "", "db");
			if (result == 0) {
				SysLogger.info("###### " + dbnode.getIpAddress() + " ���ڲɼ�ʱ�����,����######");
				return null;
			}

			String serverip = dbmonitorlist.getIpAddress();
			String username = dbmonitorlist.getUser();
			String passwords = EncryptUtil.decode(dbmonitorlist.getPassword());
			// SysLogger.info("#######################################################");
			// SysLogger.info("username:
			// "+username+"===============passwords:"+passwords);
			// SysLogger.info("#######################################################");
			int port = Integer.parseInt(dbmonitorlist.getPort());
			// String dbnames = dbmonitorlist.getDbName();
			// Date d1 = new Date();
			// �жϸ����ݿ��Ƿ���������
			boolean sysbaseIsOK = false;
			// if (dbnode.getCollecttype() ==
			// SystemConstant.DBCOLLECTTYPE_SHELL) {
			// // �ű��ɼ���ʽ
			// String filename = ResourceCenter.getInstance().getSysPath() +
			// "/linuxserver/" + serverip + ".sysbase.log";
			// File file = new File(filename);
			// if (!file.exists()) {
			// // �ļ�������,������澯
			// try {
			// createFileNotExistSMS(serverip);
			// } catch (Exception e) {
			// e.printStackTrace();
			// }
			// return;
			// }
			// SysLogger.info("###��ʼ����SysBase:" + serverip + "�����ļ�###");
			//
			// LoadSysbaseFile loadsysbase = new LoadSysbaseFile(filename);
			// Hashtable allSysbaseDatas = new Hashtable();
			// try {
			// allSysbaseDatas = loadsysbase.getSysbaseConfig();
			// } catch (Exception e) {
			// e.printStackTrace();
			// }
			// if (allSysbaseDatas != null && allSysbaseDatas.size() > 0) {
			// if (allSysbaseDatas.containsKey("status")) {
			// int status =
			// Integer.parseInt(allSysbaseDatas.get("status").toString());
			// if (status == 1)
			// sysbaseIsOK = true;
			// if (!sysbaseIsOK) {
			// // ��Ҫ�������ݿ����ڵķ������Ƿ�����ͨ
			// Host host = (Host)
			// PollingEngine.getInstance().getNodeByIP(serverip);
			// Vector ipPingData = (Vector)
			// ShareData.getPingdata().get(serverip);
			// if (ipPingData != null) {
			// Pingcollectdata pingdata = (Pingcollectdata) ipPingData.get(0);
			// Calendar tempCal = (Calendar) pingdata.getCollecttime();
			// Date cc = tempCal.getTime();
			// String time = sdf.format(cc);
			// String lastTime = time;
			// String pingvalue = pingdata.getThevalue();
			// if (pingvalue == null || pingvalue.trim().length() == 0)
			// pingvalue = "0";
			// double pvalue = new Double(pingvalue);
			// if (pvalue == 0) {
			// // �������������Ӳ���***********************************************
			// dbnode = (DBNode)
			// PollingEngine.getInstance().getDbByID(dbmonitorlist.getId());
			// dbnode.setAlarm(true);
			// dbnode.setStatus(3);
			// List alarmList = dbnode.getAlarmMessage();
			// if (alarmList == null)
			// alarmList = new ArrayList();
			// dbnode.getAlarmMessage().add("���ݿ����ֹͣ");
			// String sysLocation = "";
			// try {
			// SmscontentDao eventdao = new SmscontentDao();
			// String eventdesc = "SYBASE(" + dbmonitorlist.getDbName() + " IP:"
			// + dbmonitorlist.getIpAddress() + ")" + "�����ݿ����ֹͣ";
			// eventdao.createEventWithReasion("poll", dbmonitorlist.getId() +
			// "", dbmonitorlist
			// .getAlias()
			// + "(" + dbmonitorlist.getIpAddress() + ")", eventdesc, 3, "db",
			// "ping",
			// "���ڵķ��������Ӳ���");
			// } catch (Exception e) {
			// e.printStackTrace();
			// }
			// } else {
			// Pingcollectdata hostdata = null;
			// hostdata = new Pingcollectdata();
			// hostdata.setIpaddress(serverip);
			// Calendar date = Calendar.getInstance();
			// hostdata.setCollecttime(date);
			// hostdata.setCategory("SYSPing");
			// hostdata.setEntity("Utilization");
			// hostdata.setSubentity("ConnectUtilization");
			// hostdata.setRestype("dynamic");
			// hostdata.setUnit("%");
			// hostdata.setThevalue("0");
			// try {
			// dbdao.createHostData(hostdata);
			// // ���Ͷ���
			// dbnode = (DBNode)
			// PollingEngine.getInstance().getDbByID(dbmonitorlist.getId());
			// dbnode.setAlarm(true);
			// List alarmList = dbnode.getAlarmMessage();
			// if (alarmList == null)
			// alarmList = new ArrayList();
			// dbnode.getAlarmMessage().add("���ݿ����ֹͣ");
			// dbnode.setStatus(3);
			// createSMS("sybase", dbmonitorlist);
			// } catch (Exception e) {
			// e.printStackTrace();
			// }
			// }
			//
			// } else {
			// Pingcollectdata hostdata = null;
			// hostdata = new Pingcollectdata();
			// hostdata.setIpaddress(serverip);
			// Calendar date = Calendar.getInstance();
			// hostdata.setCollecttime(date);
			// hostdata.setCategory("SYSPing");
			// hostdata.setEntity("Utilization");
			// hostdata.setSubentity("ConnectUtilization");
			// hostdata.setRestype("dynamic");
			// hostdata.setUnit("%");
			// hostdata.setThevalue("0");
			// try {
			// dbdao.createHostData(hostdata);
			// // ���Ͷ���
			// dbnode = (DBNode)
			// PollingEngine.getInstance().getDbByID(dbmonitorlist.getId());
			// dbnode.setAlarm(true);
			// List alarmList = dbnode.getAlarmMessage();
			// if (alarmList == null)
			// alarmList = new ArrayList();
			// dbnode.getAlarmMessage().add("���ݿ����ֹͣ");
			// dbnode.setStatus(3);
			// createSMS("sybase", dbmonitorlist);
			// } catch (Exception e) {
			// e.printStackTrace();
			// }
			// }
			// } else {
			// Pingcollectdata hostdata = null;
			// hostdata = new Pingcollectdata();
			// hostdata.setIpaddress(serverip);
			// Calendar date = Calendar.getInstance();
			// hostdata.setCollecttime(date);
			// hostdata.setCategory("SYSPing");
			// hostdata.setEntity("Utilization");
			// hostdata.setSubentity("ConnectUtilization");
			// hostdata.setRestype("dynamic");
			// hostdata.setUnit("%");
			// hostdata.setThevalue("100");
			// try {
			// dbdao.createHostData(hostdata);
			// } catch (Exception e) {
			// e.printStackTrace();
			// }
			// }
			// Hashtable retValue = new Hashtable();
			// if (sysbaseIsOK) {// �����ݿ��������ϣ���������ݿ����ݵĲɼ�
			// SybaseVO sysbaseVO = new SybaseVO();
			//
			// try {
			// // sysbaseVO =
			// // dbdao.getSysbaseInfo(serverip,
			// // port, username, passwords);
			// sysbaseVO = (SybaseVO) allSysbaseDatas.get("sysbase");
			// } catch (Exception e) {
			// e.printStackTrace();
			// }
			// if (sysbaseVO == null)
			// sysbaseVO = new SybaseVO();
			//									
			// retValue.put("status", "1");
			// retValue.put("sysbaseVO", sysbaseVO);
			//									
			// List allspace = sysbaseVO.getDbInfo();
			// if (allspace != null && allspace.size() > 0) {
			// for (int k = 0; k < allspace.size(); k++) {
			// TablesVO tvo = (TablesVO) allspace.get(k);
			// if (sybspaceconfig.containsKey(serverip + ":" +
			// tvo.getDb_name())) {
			// // �澯�ж�
			// Sybspaceconfig sybconfig = (Sybspaceconfig)
			// sybspaceconfig.get(serverip + ":"
			// + tvo.getDb_name());
			// Integer usedperc = Integer.parseInt(tvo.getDb_usedperc());
			// if (usedperc > sybconfig.getAlarmvalue()) {
			// // ������ֵ�澯
			// dbnode = (DBNode) PollingEngine.getInstance()
			// .getDbByID(dbmonitorlist.getId());
			// dbnode.setAlarm(true);
			// List alarmList = dbnode.getAlarmMessage();
			// if (alarmList == null)
			// alarmList = new ArrayList();
			// dbnode.getAlarmMessage().add(
			// sybconfig.getSpacename() + "���ռ䳬����ֵ" +
			// sybconfig.getAlarmvalue());
			// dbnode.setStatus(3);
			// createSybSpaceSMS(dbmonitorlist, sybconfig);
			// }
			// }
			// }
			// }
			// }
			// if(retValue!=null)
			// ShareData.setSysbasedata(serverip, retValue);
			// }
			// }
			// // ////////////////////////////////////////////////////////////
			// } else {
			// JDBC�ɼ���ʽ
			try {
				// System.out.println(serverip+"---"+username+"---"+passwords+"---"+port);
				dbdao = new DBDao();
				sysbaseIsOK = dbdao.getSysbaseIsOk(serverip, username, passwords, port);
			} catch (Exception e) {
				e.printStackTrace();
				sysbaseIsOK = false;
			} finally {
				dbdao.close();
			}
			Hashtable retValue = new Hashtable();
			if (sysbaseIsOK) {// �����ݿ��������ϣ���������ݿ����ݵĲɼ�
				SybaseVO sysbaseVO = new SybaseVO();
				try {
					dbdao = new DBDao();
					sysbaseVO = dbdao.getSysbaseInfo(serverip, port, username, passwords, gatherHash);
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					dbdao.close();
				}
				if (sysbaseVO == null) {
					sysbaseVO = new SybaseVO();
				}

				retValue.put("sysbaseVO", sysbaseVO);
				retValue.put("status", "1");
			}
			if (retValue != null) {
				ShareData.setSysbasedata(serverip, retValue);
				// ����sybase��Ϣ�����ݿ�
				IpTranslation tranfer = new IpTranslation();
				String hex = IpTranslation.formIpToHex(serverip);
				saveSybaseData(hex + ":" + dbmonitorlist.getId(), retValue);
			}
			// }
			updateData(dbmonitorlist, ShareData.getSysbasedata());
			SysLogger.info("#### �����ɼ�SYBASE���ݿ�" + serverip + " ####");
			// }
			// }
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (dbdao != null) {
				dbdao.close();
			}
			SysLogger.info("#### sysbasetask������� ####");
		}
		return returndata;
	}

	/**
	 * ����sybase��������Ϣ
	 * 
	 * @param serverip
	 *            ��ת����IP��ַ�����ݿ�ID�� ���
	 * @param sybaseData
	 */
	public void saveSybaseData(String serverip, Hashtable sybaseData) {
		if (sybaseData == null || sybaseData.size() == 0) {
			return;
		}
		DBDao dbDao = new DBDao();
		try {
			String status = null;// ���ݿ��״̬��Ϣ
			SybaseVO sybaseVO = null;
			List dbInfo = null;// ���ݿ���Ϣ
			List deviceInfo = null;// �豸��Ϣ
			List userInfo = null;// �û���Ϣ
			List serversInfo = null;// ��������Ϣ
			List processInfo = null;// ������ϢengineInfo
			List dbsInfo = null;// ������Ϣ
			List engineInfo = null;// ������Ϣ
			if (sybaseData != null && sybaseData.containsKey("sysbaseVO")) {
				sybaseVO = (SybaseVO) sybaseData.get("sysbaseVO");
			}
			if (sybaseData != null && sybaseData.containsKey("status")) {
				status = (String) sybaseData.get("status");
				dbDao.clearTableData("nms_sybasestatus", serverip);
				dbDao.addSybase_nmsstatus(serverip, status);
			}

			if (sybaseVO != null) {
				// ����������Ϣ
				dbDao.clearTableData("nms_sybaseperformance", serverip);
				dbDao.addSybase_nmsperformance(serverip, sybaseVO);
				dbInfo = sybaseVO.getDbInfo();
				deviceInfo = sybaseVO.getDeviceInfo();
				userInfo = sybaseVO.getUserInfo();
				serversInfo = sybaseVO.getServersInfo();
				processInfo = sybaseVO.getProcessInfo();
				dbsInfo = sybaseVO.getDbsInfo();
				engineInfo = sybaseVO.getEngineInfo();

				if (dbInfo != null && dbInfo.size() > 0) {
					dbDao.clearTableData("nms_sybasedbinfo", serverip);
					dbDao.addSybase_nmsdbinfo(serverip, dbInfo);
				}
				if (dbsInfo != null && dbsInfo.size() > 0) {
					dbDao.clearTableData("nms_sybasedbdetailinfo", serverip);
					dbDao.addSybase_nmsdbdetailinfo(serverip, dbsInfo);
				}
				if (deviceInfo != null && deviceInfo.size() > 0) {
					dbDao.clearTableData("nms_sybasedeviceinfo", serverip);
					dbDao.addSybase_nmsdeviceinfo(serverip, deviceInfo);
				}
				if (processInfo != null && processInfo.size() > 0) {
					dbDao.clearTableData("nms_sybaseprocessinfo", serverip);
					dbDao.addSybase_nmsprocessinfo(serverip, processInfo);
				}
				if (userInfo != null && userInfo.size() > 0) {
					dbDao.clearTableData("nms_sybaseuserinfo", serverip);
					dbDao.addSybase_nmsuserinfo(serverip, userInfo);
				}
				if (serversInfo != null && serversInfo.size() > 0) {
					dbDao.clearTableData("nms_sybaseserversinfo", serverip);
					dbDao.addSybase_nmsserversinfo(serverip, serversInfo);
				}
				if (engineInfo != null && engineInfo.size() > 0) {
					dbDao.clearTableData("nms_sybaseengineinfo", serverip);
					dbDao.addSybase_nmsengineinfo(serverip, engineInfo);
				}
			}
			// ȡ����
			testGetSybaseDataFormDB(serverip);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			dbDao.close();
		}
	}

	// public void createFileNotExistSMS(String ipaddress) {
	// // ��������
	// // ���ڴ����õ�ǰ���IP��PING��ֵ
	// Calendar date = Calendar.getInstance();
	// try {
	// Host host = (Host) PollingEngine.getInstance().getNodeByIP(ipaddress);
	// if (host == null)
	// return;
	//
	// if (!sendeddata.containsKey(ipaddress + ":file:" + host.getId())) {
	// // �����ڣ��������ţ��������ӵ������б���
	// Smscontent smscontent = new Smscontent();
	// String time = sdf.format(date.getTime());
	// smscontent.setLevel("3");
	// smscontent.setObjid(host.getId() + "");
	// smscontent.setMessage(host.getAlias() + " (" + host.getIpAddress() + ")"
	// + "����־�ļ��޷���ȷ�ϴ������ܷ�����");
	// smscontent.setRecordtime(time);
	// smscontent.setSubtype("host");
	// smscontent.setSubentity("ftp");
	// smscontent.setIp(host.getIpAddress());// ���Ͷ���
	// SmscontentDao smsmanager = new SmscontentDao();
	// smsmanager.sendURLSmscontent(smscontent);
	// sendeddata.put(ipaddress + ":file" + host.getId(), date);
	// } else {
	// // ���ڣ�����ѷ��Ͷ����б����ж��Ƿ��Ѿ����͵���Ķ���
	// Calendar formerdate = (Calendar) sendeddata.get(ipaddress + ":file:" +
	// host.getId());
	// SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
	// Date last = null;
	// Date current = null;
	// Calendar sendcalen = formerdate;
	// Date cc = sendcalen.getTime();
	// String tempsenddate = formatter.format(cc);
	//
	// Calendar currentcalen = date;
	// cc = currentcalen.getTime();
	// last = formatter.parse(tempsenddate);
	// String currentsenddate = formatter.format(cc);
	// current = formatter.parse(currentsenddate);
	//
	// long subvalue = current.getTime() - last.getTime();
	// if (subvalue / (1000 * 60 * 60 * 24) >= 1) {
	// // ����һ�죬���ٷ���Ϣ
	// Smscontent smscontent = new Smscontent();
	// String time = sdf.format(date.getTime());
	// smscontent.setLevel("3");
	// smscontent.setObjid(host.getId() + "");
	// smscontent.setMessage(host.getAlias() + " (" + host.getIpAddress() + ")"
	// + "����־�ļ��޷���ȷ�ϴ������ܷ�����");
	// smscontent.setRecordtime(time);
	// smscontent.setSubtype("host");
	// smscontent.setSubentity("ftp");
	// smscontent.setIp(host.getIpAddress());// ���Ͷ���
	// SmscontentDao smsmanager = new SmscontentDao();
	// smsmanager.sendURLSmscontent(smscontent);
	// // �޸��Ѿ����͵Ķ��ż�¼
	// sendeddata.put(ipaddress + ":file:" + host.getId(), date);
	// }
	// }
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	// }

	public void testGetSybaseDataFormDB(String serverip) {
		// String status = null;//���ݿ��״̬��Ϣ
		SybaseVO sybaseVO = null;
		List dbInfo = null;// ���ݿ���Ϣ
		List deviceInfo = null;// �豸��Ϣ
		List userInfo = null;// �û���Ϣ
		List serversInfo = null;// ��������Ϣ
		DBDao dao = new DBDao();
		try {
			// Hashtable tempStatusHashtable =
			// dao.getSybase_nmsstatus(serverip);
			// if(tempStatusHashtable != null &&
			// tempStatusHashtable.containsKey("status")){
			// status = (String)tempStatusHashtable.get("status");
			// }
			sybaseVO = dao.getSybase_nmssybaseperformance(serverip);
			dbInfo = dao.getSybase_nmsdbinfo(serverip);
			deviceInfo = dao.getSybase_nmsdeviceinfo(serverip);
			userInfo = dao.getSybase_nmsuserinfo(serverip);
			serversInfo = dao.getSybase_nmsserversinfo(serverip);
			sybaseVO.setDbInfo(dbInfo);
			sybaseVO.setDeviceInfo(deviceInfo);
			sybaseVO.setUserInfo(userInfo);
			sybaseVO.setServersInfo(serversInfo);
			System.out.println("aaaaa");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			dao.close();
		}

	}

	/**
	 * @author HONGLI ���¸澯��Ϣ
	 * @param vo
	 *            ���ݿ�ʵ��
	 * @param collectingData
	 *            ���ݿ�ʵ���еĸ���������Ϣ
	 */
	public void updateData(Object vo, Object collectingData) {
		// SysLogger.info("##############updateDate ��ʼ###########");
		DBVo sybaseServer = (DBVo) vo;
		Hashtable datahashtable = (Hashtable) collectingData;
		// SysLogger.info("######HONG
		// sybaseServer.getIpAddress()--"+sybaseServer.getIpAddress());
		Hashtable sysbasehashtable = (Hashtable) datahashtable.get(sybaseServer.getIpAddress());// �õ��ɼ�sysbaseVO���ݿ����Ϣ
		SybaseVO sybaseVO = (SybaseVO) sysbasehashtable.get("sysbaseVO");
		// SysLogger.info("######HONG
		// sybaseVO--"+sybaseVO.getProcedure_hitrate());
		AlarmIndicatorsUtil alarmIndicatorsUtil = new AlarmIndicatorsUtil();
		List list = alarmIndicatorsUtil.getAlarmInicatorsThresholdForNode(String.valueOf(sybaseServer.getId()), AlarmConstant.TYPE_DB, "sybase");// ��ȡ�ɼ�ָ���б�
		// SysLogger.info("##############HONG
		// Sybase--list.size--"+list.size()+"###########");
		String serverip = sybaseServer.getIpAddress();
		CheckEventUtil checkEventUtil = new CheckEventUtil();
		NodeUtil nodeUtil = new NodeUtil();
		NodeDTO nodeDTO = nodeUtil.conversionToNodeDTO(sybaseServer);
		for (int i = 0; i < list.size(); i++) {
			AlarmIndicatorsNode alarmIndicatorsNode = (AlarmIndicatorsNode) list.get(i);
			// SysLogger.info("##############HONG
			// alarmIndicatorsNode.getEnabled--"+alarmIndicatorsNode.getEnabled()+"###########");
			if ("1".equals(alarmIndicatorsNode.getEnabled())) {
				String indicators = alarmIndicatorsNode.getName();
				String value = "";// value ��ָʵ�����ݿ��е�ֵ���� ������������ HONGLI
				// SysLogger.info("##############HONG
				// sybase-indicators--"+indicators+"##########");
				if ("procedure_cache".equals(indicators)) {
					value = sybaseVO.getProcedure_hitrate();
					// SysLogger.info("#######HONG
					// sybase-sybaseVO.getProcedure_hitrate()-->
					// "+sybaseVO.getProcedure_hitrate()+"");
				} else if ("cpu_busy_rate".equals(indicators)) {
					value = sybaseVO.getCpu_busy_rate();
				} else if ("io_busy_rate".equals(indicators)) {
					value = sybaseVO.getIo_busy_rate();
				} else if ("locks_count".equals(indicators)) {
					value = sybaseVO.getLocks_count();
				} else if ("data_hitrate".equals(indicators)) {
					value = sybaseVO.getData_hitrate();// key
														// ��nms_alarm_indicators_node
														// ��sybase��Ӧ��nameһ��
				} else if ("tablespace".equalsIgnoreCase(indicators)) {
					List allspace = sybaseVO.getDbInfo();
					SybspaceconfigDao sybspaceconfigManager = new SybspaceconfigDao();
					Hashtable sybspaceconfig = new Hashtable();
					try {
						sybspaceconfig = sybspaceconfigManager.getByAlarmflag(1);
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						sybspaceconfigManager.close();
					}
					if (allspace != null && allspace.size() > 0) {
						for (int k = 0; k < allspace.size(); k++) {
							TablesVO tvo = (TablesVO) allspace.get(k);
							if (sybspaceconfig.containsKey(serverip + ":" + tvo.getDb_name())) {
								// �澯�ж�
								Sybspaceconfig sybconfig = (Sybspaceconfig) sybspaceconfig.get(serverip + ":" + tvo.getDb_name());
								Integer usedperc = Integer.parseInt(tvo.getDb_usedperc());
								alarmIndicatorsNode.setLimenvalue0(sybconfig.getAlarmvalue() + "");
								alarmIndicatorsNode.setLimenvalue1(sybconfig.getAlarmvalue() + "");
								alarmIndicatorsNode.setLimenvalue2(sybconfig.getAlarmvalue() + "");
								checkEventUtil.checkEvent(nodeDTO, alarmIndicatorsNode, usedperc + "", tvo.getDb_name());
							}
						}
					}
				} else {
					continue;
				}
				// SysLogger.info("#######HONG
				// sybase-indicator��value--"+indicators+"��"+value+"####");
				if (value == null) {
					continue;
				}
				checkEventUtil.checkEvent(nodeDTO, alarmIndicatorsNode, value, null);
			}

		}
	}
}