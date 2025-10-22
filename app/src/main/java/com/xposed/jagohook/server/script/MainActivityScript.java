package com.xposed.jagohook.server.script;

import android.graphics.Rect;

import com.xposed.jagohook.runnable.response.CollectBillResponse;
import com.xposed.jagohook.server.SuShellService;
import com.xposed.jagohook.storage.DataStorage;
import com.xposed.jagohook.utils.Logs;
import com.xposed.jagohook.utils.NodeScriptUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainActivityScript extends BaseScript {

    @Override
    public void onCreate(SuShellService suShellService, List<SuShellService.UiXmlParser.Node> nodes) {
        Map<String, SuShellService.UiXmlParser.Node> map = NodeScriptUtils.toContentDescMap(nodes);
        inputPassword(suShellService, map);
        getBalance(suShellService, map);
        clickDialog(suShellService, map);
        homeClick(suShellService, map, nodes);
        getBill(suShellService, map, nodes);
        SelectBank(suShellService, map, nodes);
    }

    //转账
    private void SelectBank(SuShellService suShellService, Map<String, SuShellService.UiXmlParser.Node> map, List<SuShellService.UiXmlParser.Node> nodes) {
        CollectBillResponse collectBillResponse = suShellService.getCollectBillResponse();
        if (map.containsKey("Pilih Bank\n" +
                "Tab 1 dari 3")) {
            if (collectBillResponse == null) {
                suShellService.back();
            } else {
                SuShellService.UiXmlParser.Node node = map.get("Search Text Field");
                suShellService.click(node.getBounds());
                suShellService.input(collectBillResponse.getBank());
            }
        }
        if (map.containsKey("Bank tujuan tidak merespon")) {
            suShellService.getLogWindow().print("卡号错误");
            suShellService.setCollectBillResponse(null);
            suShellService.back();
        }

        if (map.containsKey("Akun tidak ditemukan")) {//卡号错误
            suShellService.getLogWindow().print("卡号错误");
            suShellService.setCollectBillResponse(null);
            suShellService.back();
        }

        if (collectBillResponse != null && map.containsKey(collectBillResponse.getBank() + "\n" +
                "BI-FAST")) {//选择银行
            SuShellService.UiXmlParser.Node node = map.get(collectBillResponse.getBank() + "\n" +
                    "BI-FAST");
            suShellService.click(node.getBounds());
        } else if (collectBillResponse != null && map.containsKey("Periksa") && map.containsKey(collectBillResponse.getBank())) {
            SuShellService.UiXmlParser.Node node = map.get("Search Text Field");
            suShellService.click(node.getBounds());

            suShellService.input(collectBillResponse.getPhone());
        } else {
            List<SuShellService.UiXmlParser.Node> nodes1 = getEndNodes(nodes, "BI-FAST");
            if (!nodes1.isEmpty()) {
                suShellService.back();
            }
            if (map.containsKey("Periksa") && collectBillResponse == null) {
                suShellService.back();
            }
        }
    }

    //获取账单
    private void getBill(SuShellService suShellService, Map<String, SuShellService.UiXmlParser.Node> map, List<SuShellService.UiXmlParser.Node> nodes) {
        if (map.containsKey("Aktivitas Semua Kantong")) {
            List<SuShellService.UiXmlParser.Node> nodes1 = getStartNodes(nodes, "Transaction Item");
            if (!nodes1.isEmpty()) {
                for (SuShellService.UiXmlParser.Node node : nodes1) {
                    Logs.d("节点：" + node.getContentDesc());
                }
            }
            DataStorage.getInstance().setHome(false);
            suShellService.back();
        }
    }


    private List<SuShellService.UiXmlParser.Node> getEndNodes(List<SuShellService.UiXmlParser.Node> nodes, String text) {
        List<SuShellService.UiXmlParser.Node> list = new ArrayList<>();
        for (SuShellService.UiXmlParser.Node node : nodes) {
            if (node.getContentDesc().endsWith(text)) {
                list.add(node);
            }
        }
        return list;
    }

    private List<SuShellService.UiXmlParser.Node> getStartNodes(List<SuShellService.UiXmlParser.Node> nodes, String text) {
        List<SuShellService.UiXmlParser.Node> list = new ArrayList<>();
        for (SuShellService.UiXmlParser.Node node : nodes) {
            if (node.getContentDesc().startsWith(text)) {
                list.add(node);
            }
        }
        return list;
    }

    //首页
    private void homeClick(SuShellService suShellService, Map<String, SuShellService.UiXmlParser.Node> map, List<SuShellService.UiXmlParser.Node> nodes) {
        DataStorage dataStorage = DataStorage.getInstance();
        if (map.containsKey("Aktivitas Terakhir")) {
            if (dataStorage.isHome()) {
                if (map.containsKey("Transaksi\n" +
                        "Tab 3 dari 5")) {
                    SuShellService.UiXmlParser.Node node = map.get("Transaksi\n" +
                            "Tab 3 dari 5");
                    suShellService.click(node.getBounds());
                }
            } else {
                dataStorage.setHome(true);
            }
        } else if (map.containsKey("Topup\n" +
                "e-Wallet")) {
            if (suShellService.getCollectBillResponse() != null) {//转账
                SuShellService.UiXmlParser.Node Transfer = map.get("Bank\n" +
                        "Transfer");
                if (Transfer != null) {
                    suShellService.click(Transfer.getBounds());
                }
            } else if (dataStorage.isHome()) {
                Logs.d("进入账单");
                //进入账单
                SuShellService.UiXmlParser.Node naf = toNAF(suShellService, nodes);
                if (naf != null) {
                    suShellService.click(naf.getBounds());
                }
            } else {
                SuShellService.UiXmlParser.Node node = map.get("Beranda\n" +
                        "Tab 1 dari 5");
                suShellService.click(node.getBounds());
            }
        }
    }

    private SuShellService.UiXmlParser.Node toNAF(SuShellService suShellService, List<SuShellService.UiXmlParser.Node> nodes) {
        for (SuShellService.UiXmlParser.Node node : nodes) {
            if (node.getNaf() != null) {
                return node;
            }
        }
        return null;
    }

    //弹窗
    private void clickDialog(SuShellService suShellService, Map<String, SuShellService.UiXmlParser.Node> map) {
        if (map.containsKey("Sesi berakhir")) {
            if (map.containsKey("Oke ")) {
                SuShellService.UiXmlParser.Node Oke = map.get("Oke ");
                suShellService.click(Oke.getBounds());
            }
        }
    }

    //获取余额
    public void getBalance(SuShellService suShellService, Map<String, SuShellService.UiXmlParser.Node> map) {
        if (map.containsKey("Aktivitas Terakhir")) {
            SuShellService.UiXmlParser.Node node = getStartTextNode(map, "Rp");
            if (node == null) return;
            String balance = node.getContentDesc();
            String numbersOnly = balance.replaceAll("[^0-9]", "");
            suShellService.setBalance(numbersOnly);
            Logs.d("余额：" + balance);
        }
    }

    public SuShellService.UiXmlParser.Node getStartTextNode(Map<String, SuShellService.UiXmlParser.Node> map, String text) {
        for (String key : map.keySet()) {
            if (key.startsWith(text)) {
                return map.get(key);
            }
        }
        return null;
    }

    //输入密码
    private void inputPassword(SuShellService suShellService, Map<String, SuShellService.UiXmlParser.Node> map) {
        if (map.containsKey("GUNAKAN PASSWORD")) {
            String pass = "115599";
            List<Rect> rects = new ArrayList<>();
            for (int i = 0; i < pass.length(); i++) {
                String key = String.valueOf(pass.charAt(i));
                if (map.containsKey(key)) {
                    SuShellService.UiXmlParser.Node node = map.get(key);
                    rects.add(node.getBounds());
                }
            }
            if (rects.isEmpty()) {
                return;
            }
            suShellService.click(rects);
        }
    }
}
