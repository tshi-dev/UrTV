package sa.urtv.home.tilawah;

/** 4.4: a sound's length as the screens say it: «٤٥ ثانية», «٣ دقائق», «٢٢ دقيقة» */
final class AudioText {
    private AudioText() { }

    static String length(long ms) {
        long s = Math.max(1, Math.round(ms / 1000.0));
        if (s < 60) return sa.urtv.home.S.t("%s ثانية", Ui.arabic((int) s));
        int m = (int) Math.round(s / 60.0);
        return m >= 3 && m <= 10 ? sa.urtv.home.S.t("%s دقائق", Ui.arabic(m)) : sa.urtv.home.S.t("%s دقيقة", Ui.arabic(m));
    }
}
