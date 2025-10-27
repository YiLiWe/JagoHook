package com.xposed.jagohook.utils;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;

public class BankUtils {
    @Getter
    private final static Map<String, String> bankMap = new HashMap<>() {{
        // 分割线上方银行（Key）→ 分割线下方银行（Value）
       put("OCBC", "OCBC NISP"); // 按常见对应关系匹配，若实际不同可调整
       put("BANK BCA", "BCA");
       put("BANK CIMB NIAGA", "CIMB Niaga");
       put("BANK BRI", "BRI");
       put("BANK MANDIRI", "Mandiri");
       put("BANK BNI", "BNI");
       put("BANK SEABANK INDONESIA", "Seabank");
       put("BANK SYARIAH INDONESIA", "Bank Syariah Indonesia");
       put("BANK PERMATA", "Permata Bank");
       put("BANK BTN", "BTN");
       //put("BANK JAGO", "Nomor Telepon atau Email"); // 下方无完全匹配“BANK JAGO”，按实际需求调整
       put("BANK ACEH SYARIAH", "BPD Aceh Syariah");
       put("BANK ALADIN", "Bank Aladin Syariah");
       put("BANK AMAR INDONESIA", "Bank Amar");
       put("BANK ANZ", "ANZ");
       put("BANK ARTHA GRAHA", "Bank Artha Graha");
       put("BANK BANTEN", "Bank Banten");
       put("BANK BCA SYARIAH", "BCA Syariah");
       put("BANK BENGKULU", "Bank Bengkulu");
       put("BANK BJB", "BJB");
       put("BANK BJB SYARIAH", "BJB Syariah");
       put("BANK BNP PARIBAS", "BNP Paribas");
       put("BANK BPR KS", "BPR KS");
       put("BANK BTN SYARIAH", "BTN Syariah");
       put("BANK BTPN SYARIAH", "BTPN Syariah");
       put("BANK BUKOPIN SYARIAH", "Bank Syariah Bukopin");
       put("BANK BUMI ARTA", "Bank Bumi Arta");
       put("BANK CAPITAL", "Bank Capital");
       put("BANK CHINATRUST", "CTBC Indonesia"); // 常见对应关系，可调整
       put("BANK CIMB NIAGA SYARIAH", "CIMB Syariah");
       put("BANK DANAMON", "Danamon");
       put("BANK DANAMON SYARIAH", "Bank Danamon Syariah");
       put("BANK DBS", "DBS");
       //put("BANK DEUTSCHE", "无对应匹配值"); // 下方无完全匹配项
       put("BANK DIGITAL BCA", "BCA Digital");
       put("BANK DKI", "Bank DKI");
       put("BANK DKI SYARIAH", "Bank DKI Syariah");
       put("BANK EKA", "BPR Eka");
       put("BANK GANESHA", "Bank Ganesha");
       put("BANK ICBC", "ICBC");
       put("BANK INA PERDANA", "Bank Ina");
       put("BANK INDEX", "Bank Index");
       //put("BANK INTERIM INDONESIA", "无对应匹配值"); // 下方无完全匹配项
      // put("BANK JAGO UU SYARIAH", "无对应匹配值"); // 下方无完全匹配项
       //ut("BANK JASA JAKARTA", "无对应匹配值"); // 下方无完全匹配项*/
       put("BANK JATENG", "Bank Jateng");
       put("BANK JATENG SYARIAH", "Bank Jateng Syariah");
       put("BANK JATIM", "Bank Jatim");
       put("BANK JATIM SYARIAH", "Bank Jatim Syariah");
       put("BANK JTRUST INDONESIA", "JTrust Bank");
       put("BANK KALBAR", "Bank Kalbar");
       put("BANK KALBAR SYARIAH", "Bank Kalbar Syariah");
       put("BANK KALTENG", "Bank Kalteng");
       put("BANK KB BUKOPIN", "Bank KB Bukopin");
       put("BANK KEB HANA", "KEB Hana Bank");
       put("BANK MANDIRI TASPEN", "Bank Mandiri Taspen");
       //put("BANK MAS", "无对应匹配值"); // 下方无完全匹配项
       put("BANK MASPION INDONESIA", "Bank Maspion");
       put("BANK MAYAPADA", "Bank Mayapada");
       put("BANK MAYBANK", "Maybank");
       put("BANK MAYBANK UU SYARIAH", "Maybank Syariah");
       put("BANK MEGA", "Bank Mega");
       put("BANK MEGA SYARIAH", "Mega Syariah");
       put("BANK MESTIKA DHARMA", "Bank Mestika");
       put("BANK MIZUHO", "Bank Mizuho");
       put("BANK MNC INTERNASIONAL", "MNC Bank");
       put("BANK MUAMALAT", "Bank Muamalat");
       put("BANK NAGARI", "Bank Nagari");
       put("BANK NAGARI SYARIAH", "Bank Nagari Syariah");
       put("BANK NEO COMMERCE", "Bank Neo Commerce");
       put("BANK NOBU", "Bank National Nobu");
       put("BANK OF AMERICA", "Bank of America");
       put("BANK OF CHINA (HKG)", "Bank of China");
       put("BANK OF INDIA INDONESIA", "Bank of India Indonesia");
       //put("BANK OF TOKYO MITSUBISHI UFJ", "MUFG Bank"); // 常见对应关系，可调整
       put("BANK OKE", "Bank OKE");
       put("BANK PANIN", "Bank Panin");
       put("BANK PANIN SYARIAH", "Bank Panin Syariah");
       put("BANK PERMATA SYARIAH", "Bank Permata Syariah");
       //put("BANK ONB INDONESIA", "无对应匹配值"); // 下方无完全匹配项
       put("BANK RAYA INDONESIA", "Bank Raya Indonesia");
       put("BANK RESONA PERDANIA", "Bank Resona Perdania");
       put("BANK SAHABAT SAMPOERNA", "Bank Sahabat Sampoerna");
       put("BANK SBIINDONESIA", "Bank SBI Indonesia");
       put("BANK SHINHAN INDONESIA", "Shinhan Bank");
       put("BANK SINARMAS", "Bank Sinarmas");
       put("BANK SUMSEL BABEL", "Bank Sumsel Babel");
       put("BANK SUMSEL BABEL SYARIAH", "Bank Sumsel Babel Syaria");
       put("BANK UOB", "UOB");
       put("BANK VICTORIA", "Bank Victoria");
       put("BANK VICTORIA SYARIAH", "Bank Victoria Syariah");
       put("BANK WOORI SAUDARA", "Bank Woori Saudara");
       put("BPD BALI", "BPD Bali");
       put("BPD DIY", "BPD DIY");
       put("BPD DIY SYARIAH", "BPD DIY Syariah");
       put("BPD JAMBI", "Bank Jambi");
       put("BPD KALSEL", "Bank Kalsel");
       put("BPD KALSEL SYARIAH", "Bank Kalsel Syariah");
       put("BPD KALTIM KALTARA", "BPD Kaltimtara");
       put("BPD KALTIM SYARIAH", "BPD Kaltimtara Syariah");
       put("BPD LAMPUNG", "Bank Lampung");
       put("BPD MALUKU", "Bank Maluku & Maluku Utara");
       //put("BPD NTBS", "BPD NTB Syariah"); // 按常见对应关系，可调整
       put("BPD NTT", "Bank NTT");
       put("BPD PAPUA", "Bank Papua");
       put("BPD RIAU KEPRI", "Bank Riau");
       put("BPD SULAWESI SELATAN", "Bank Sulselbar");
       put("BPD SULAWESI TENGAH", "BPD Sulteng");
       put("BPD SULAWESI TENGGARA", "BPD Sultra");
       put("BPD SUMUT", "BPD Sumut");
       put("BPD SUMUT SYARIAH", "BPD Sumut Syariah");
     /*p.put("BPR DANAGUNG ABADI", "无对应匹配值"); // 下方无完全匹配项
       put("BPR DANAGUNG BAKTI", "无对应匹配值"); // 下方无完全匹配项
       put("BPR DANAGUNG RAMULTI", "无对应匹配值"); // 下方无完全匹配项*/
       put("BPR SUPRA", "BPR Supra");
       put("Bank Krom Indonesia", "Krom Bank");
       put("Bank SulutGo", "Bank SulutGo");
       put("Bank Super indonesia", "Superbank");
       put("CITIBANK", "Citibank");
       put("DOKU", "DOKU");
       put("HSBC INDONESIA", "HSBC");
       put("JPMORGAN CHASE BANK", "JPMorgan Chase");
       put("LINKAJA", "LINKAJA");
       put("PAYPRO", "PAYPRO");
       put("PT ALLO BANK INDONESIA", "Bank Allo");
       put("PT BANK NANO SYARIAH", "Bank Nano Syariah");
       put("PT BANK SMBC INDONESIA TBK", "Bank SMBC Indonesia");
       put("PT Bank Hibank Indonesia", "Hibank");
       //put("PT. BANK CHINA CONSTRUCTION INDONESIA(CCBI)", "Bank CCB Indonesia");
       put("PT. BANK IBK INDONESIA", "IBK Bank");
       put("STANDARD CHARTERED", "Standard Chartered");
    }};
}
