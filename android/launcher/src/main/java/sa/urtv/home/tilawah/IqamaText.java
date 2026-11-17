package sa.urtv.home.tilawah;

import sa.urtv.home.S;

/** 4.4 «تنبيه الإقامة»'s words: «حان وقت إقامة صلاة العصر» */
final class IqamaText {
    private IqamaText() { }

    static String line(int prayer) {
        if (prayer < 0 || prayer >= PrayerTimes.NAMES.length) return S.t("حان وقت الإقامة");
        return S.t("حان وقت إقامة صلاة %s", S.tv(PrayerTimes.NAMES[prayer]));
    }
}
