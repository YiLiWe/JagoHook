package com.xposed.jagohook.utils;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;

public class BankUtils {
    // 定义HashMap存储银行名称映射（Key：分割线上方银行，Value：分割线下方银行）
    @Getter
    private static Map<String, String> bankMap = new HashMap<>();

    static {
        initBankMapping();
    }

    /**
     * 初始化银行名称映射（Key：分割线上方银行，Value：分割线下方对应银行）
     * 注：按“完全匹配”规则建立映射，无匹配项时Value设为“无对应匹配值”
     */
    private static void initBankMapping() {
        // 分割线上方银行（Key）→ 分割线下方银行（Value）
        bankMap.put("OCBC", "OCBC NISP"); // 按常见对应关系匹配，若实际不同可调整
        bankMap.put("BANK BCA", "BCA");
        bankMap.put("BANK CIMB NIAGA", "CIMB Niaga");
        bankMap.put("BANK BRI", "BRI");
        bankMap.put("BANK MANDIRI", "Mandiri");
        bankMap.put("BANK BNI", "BNI");
        bankMap.put("BANK SEABANK INDONESIA", "Seabank");
        bankMap.put("BANK SYARIAH INDONESIA", "Bank Syariah Indonesia");
        bankMap.put("BANK PERMATA", "Permata Bank");
        bankMap.put("BANK BTN", "BTN");
        // bankMap.put("BANK JAGO", "Nomor Telepon atau Email"); // 下方无完全匹配“BANK JAGO”，按实际需求调整
        bankMap.put("BANK ACEH SYARIAH", "BPD Aceh Syariah");
        bankMap.put("BANK ALADIN", "Bank Aladin Syariah");
        bankMap.put("BANK AMAR INDONESIA", "Bank Amar");
        bankMap.put("BANK ANZ", "ANZ");
        bankMap.put("BANK ARTHA GRAHA", "Bank Artha Graha");
        bankMap.put("BANK BANTEN", "Bank Banten");
        bankMap.put("BANK BCA SYARIAH", "BCA Syariah");
        bankMap.put("BANK BENGKULU", "Bank Bengkulu");
        bankMap.put("BANK BJB", "BJB");
        bankMap.put("BANK BJB SYARIAH", "BJB Syariah");
        bankMap.put("BANK BNP PARIBAS", "BNP Paribas");
        bankMap.put("BANK BPR KS", "BPR KS");
        bankMap.put("BANK BTN SYARIAH", "BTN Syariah");
        bankMap.put("BANK BTPN SYARIAH", "BTPN Syariah");
        bankMap.put("BANK BUKOPIN SYARIAH", "Bank Syariah Bukopin");
        bankMap.put("BANK BUMI ARTA", "Bank Bumi Arta");
        bankMap.put("BANK CAPITAL", "Bank Capital");
        bankMap.put("BANK CHINATRUST", "CTBC Indonesia"); // 常见对应关系，可调整
        bankMap.put("BANK CIMB NIAGA SYARIAH", "CIMB Syariah");
        bankMap.put("BANK DANAMON", "Danamon");
        bankMap.put("BANK DANAMON SYARIAH", "Bank Danamon Syariah");
        bankMap.put("BANK DBS", "DBS");
        //  bankMap.put("BANK DEUTSCHE", "无对应匹配值"); // 下方无完全匹配项
        bankMap.put("BANK DIGITAL BCA", "BCA Digital");
        bankMap.put("BANK DKI", "Bank DKI");
        bankMap.put("BANK DKI SYARIAH", "Bank DKI Syariah");
        bankMap.put("BANK EKA", "BPR Eka");
        bankMap.put("BANK GANESHA", "Bank Ganesha");
        bankMap.put("BANK ICBC", "ICBC");
        bankMap.put("BANK INA PERDANA", "Bank Ina");
        bankMap.put("BANK INDEX", "Bank Index");
       /* bankMap.put("BANK INTERIM INDONESIA", "无对应匹配值"); // 下方无完全匹配项
        bankMap.put("BANK JAGO UU SYARIAH", "无对应匹配值"); // 下方无完全匹配项
        bankMap.put("BANK JASA JAKARTA", "无对应匹配值"); // 下方无完全匹配项*/
        bankMap.put("BANK JATENG", "Bank Jateng");
        bankMap.put("BANK JATENG SYARIAH", "Bank Jateng Syariah");
        bankMap.put("BANK JATIM", "Bank Jatim");
        bankMap.put("BANK JATIM SYARIAH", "Bank Jatim Syariah");
        bankMap.put("BANK JTRUST INDONESIA", "JTrust Bank");
        bankMap.put("BANK KALBAR", "Bank Kalbar");
        bankMap.put("BANK KALBAR SYARIAH", "Bank Kalbar Syariah");
        bankMap.put("BANK KALTENG", "Bank Kalteng");
        bankMap.put("BANK KB BUKOPIN", "Bank KB Bukopin");
        bankMap.put("BANK KEB HANA", "KEB Hana Bank");
        bankMap.put("BANK MANDIRI TASPEN", "Bank Mandiri Taspen");
        // bankMap.put("BANK MAS", "无对应匹配值"); // 下方无完全匹配项
        bankMap.put("BANK MASPION INDONESIA", "Bank Maspion");
        bankMap.put("BANK MAYAPADA", "Bank Mayapada");
        bankMap.put("BANK MAYBANK", "Maybank");
        bankMap.put("BANK MAYBANK UU SYARIAH", "Maybank Syariah");
        bankMap.put("BANK MEGA", "Bank Mega");
        bankMap.put("BANK MEGA SYARIAH", "Mega Syariah");
        bankMap.put("BANK MESTIKA DHARMA", "Bank Mestika");
        bankMap.put("BANK MIZUHO", "Bank Mizuho");
        bankMap.put("BANK MNC INTERNASIONAL", "MNC Bank");
        bankMap.put("BANK MUAMALAT", "Bank Muamalat");
        bankMap.put("BANK NAGARI", "Bank Nagari");
        bankMap.put("BANK NAGARI SYARIAH", "Bank Nagari Syariah");
        bankMap.put("BANK NEO COMMERCE", "Bank Neo Commerce");
        bankMap.put("BANK NOBU", "Bank National Nobu");
        bankMap.put("BANK OF AMERICA", "Bank of America");
        bankMap.put("BANK OF CHINA (HKG)", "Bank of China");
        bankMap.put("BANK OF INDIA INDONESIA", "Bank of India Indonesia");
        //  bankMap.put("BANK OF TOKYO MITSUBISHI UFJ", "MUFG Bank"); // 常见对应关系，可调整
        bankMap.put("BANK OKE", "Bank OKE");
        bankMap.put("BANK PANIN", "Bank Panin");
        bankMap.put("BANK PANIN SYARIAH", "Bank Panin Syariah");
        bankMap.put("BANK PERMATA SYARIAH", "Bank Permata Syariah");
        //bankMap.put("BANK ONB INDONESIA", "无对应匹配值"); // 下方无完全匹配项
        bankMap.put("BANK RAYA INDONESIA", "Bank Raya Indonesia");
        bankMap.put("BANK RESONA PERDANIA", "Bank Resona Perdania");
        bankMap.put("BANK SAHABAT SAMPOERNA", "Bank Sahabat Sampoerna");
        bankMap.put("BANK SBIINDONESIA", "Bank SBI Indonesia");
        bankMap.put("BANK SHINHAN INDONESIA", "Shinhan Bank");
        bankMap.put("BANK SINARMAS", "Bank Sinarmas");
        bankMap.put("BANK SUMSEL BABEL", "Bank Sumsel Babel");
        bankMap.put("BANK SUMSEL BABEL SYARIAH", "Bank Sumsel Babel Syaria");
        bankMap.put("BANK UOB", "UOB");
        bankMap.put("BANK VICTORIA", "Bank Victoria");
        bankMap.put("BANK VICTORIA SYARIAH", "Bank Victoria Syariah");
        bankMap.put("BANK WOORI SAUDARA", "Bank Woori Saudara");
        bankMap.put("BPD BALI", "BPD Bali");
        bankMap.put("BPD DIY", "BPD DIY");
        bankMap.put("BPD DIY SYARIAH", "BPD DIY Syariah");
        bankMap.put("BPD JAMBI", "Bank Jambi");
        bankMap.put("BPD KALSEL", "Bank Kalsel");
        bankMap.put("BPD KALSEL SYARIAH", "Bank Kalsel Syariah");
        bankMap.put("BPD KALTIM KALTARA", "BPD Kaltimtara");
        bankMap.put("BPD KALTIM SYARIAH", "BPD Kaltimtara Syariah");
        bankMap.put("BPD LAMPUNG", "Bank Lampung");
        bankMap.put("BPD MALUKU", "Bank Maluku & Maluku Utara");
        // bankMap.put("BPD NTBS", "BPD NTB Syariah"); // 按常见对应关系，可调整
        bankMap.put("BPD NTT", "Bank NTT");
        bankMap.put("BPD PAPUA", "Bank Papua");
        bankMap.put("BPD RIAU KEPRI", "Bank Riau");
        bankMap.put("BPD SULAWESI SELATAN", "Bank Sulselbar");
        bankMap.put("BPD SULAWESI TENGAH", "BPD Sulteng");
        bankMap.put("BPD SULAWESI TENGGARA", "BPD Sultra");
        bankMap.put("BPD SUMUT", "BPD Sumut");
        bankMap.put("BPD SUMUT SYARIAH", "BPD Sumut Syariah");
     /*   bankMap.put("BPR DANAGUNG ABADI", "无对应匹配值"); // 下方无完全匹配项
        bankMap.put("BPR DANAGUNG BAKTI", "无对应匹配值"); // 下方无完全匹配项
        bankMap.put("BPR DANAGUNG RAMULTI", "无对应匹配值"); // 下方无完全匹配项*/
        bankMap.put("BPR SUPRA", "BPR Supra");
        bankMap.put("Bank Krom Indonesia", "Krom Bank");
        bankMap.put("Bank SulutGo", "Bank SulutGo");
        bankMap.put("Bank Super indonesia", "Superbank");
        bankMap.put("CITIBANK", "Citibank");
        bankMap.put("DOKU", "DOKU");
        bankMap.put("HSBC INDONESIA", "HSBC");
        bankMap.put("JPMORGAN CHASE BANK", "JPMorgan Chase");
        bankMap.put("LINKAJA", "LINKAJA");
        bankMap.put("PAYPRO", "PAYPRO");
        bankMap.put("PT ALLO BANK INDONESIA", "Bank Allo");
        bankMap.put("PT BANK NANO SYARIAH", "Bank Nano Syariah");
        bankMap.put("PT BANK SMBC INDONESIA TBK", "Bank SMBC Indonesia");
        bankMap.put("PT Bank Hibank Indonesia", "Hibank");
       // bankMap.put("PT. BANK CHINA CONSTRUCTION INDONESIA(CCBI)", "Bank CCB Indonesia");
        bankMap.put("PT. BANK IBK INDONESIA", "IBK Bank");
        bankMap.put("STANDARD CHARTERED", "Standard Chartered");
    }

}
