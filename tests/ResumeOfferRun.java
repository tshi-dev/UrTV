package sa.urtv.home.tilawah;

/** For tests/resume_offer_test.py: ResumeOffer's rules, one "name<TAB>got" line per check. */
public final class ResumeOfferRun {
    public static void main(String[] a) {
        long now = 100L * 3600_000L, h = 60_000L;
        System.out.println("radio cut\t" + ResumeOffer.due(now - h, now, false, ResumeOffer.RADIO));
        System.out.println("surah cut\t" + ResumeOffer.due(now - h, now, false, ResumeOffer.SURAH));
        System.out.println("never started\t" + ResumeOffer.due(0, now, false, ResumeOffer.RADIO));
        System.out.println("playing now\t" + ResumeOffer.due(now - h, now, true, ResumeOffer.RADIO));
        System.out.println("adhan or none\t" + ResumeOffer.due(now - h, now, false, 3) + " " + ResumeOffer.due(now - h, now, false, 0));
        System.out.println("too old\t" + ResumeOffer.due(now - 11 * h, now, false, ResumeOffer.RADIO) + " " + ResumeOffer.due(now - 10 * h, now, false, ResumeOffer.RADIO));
        System.out.println("future\t" + ResumeOffer.due(now + h, now, false, ResumeOffer.RADIO));
    }
}
