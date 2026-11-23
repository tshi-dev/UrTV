package sa.urtv.home;

/**
 * 4.4 beta 4 (the owner: «كنبة» as couch-tv's own design, «take all the icons, as if we took them from the app itself»): couch-tv's app
 * faces (tv-home/web/app-icons, MIT License, Copyright (c) 2026 Seagey; THIRD_PARTY_NOTICES.md), shown for the «أيقونات» way in place of
 * an installed app's own icon. The marks are their owners'; a face is shown only to launch that app, already installed on the TV.
 * ONE table: to take a face away (a brand objects), delete its line here and its file in assets/couch-faces/. No Android in here.
 */
final class CouchFaces {
    private CouchFaces() { }

    /** package name, face (assets/couch-faces/<face>.png) */
    static final String[][] FACES = {
        {"au.com.tenplay", "10play"},   // 10 Play
        {"com.swm.live", "7plus"},   // 7plus
        {"au.com.ninenow.ctv", "9now"},   // 9Now
        {"com.apple.atve.androidtv.appletv", "appletv"},   // Apple TV
        {"au.com.streamotion.ares.tv", "binge"},   // Binge
        {"com.crunchyroll.crunchyroid", "crunchyroll"},   // Crunchyroll
        {"com.dazn", "dazn"},   // DAZN
        {"com.disney.disneyplus", "disney"},   // Disney+
        {"au.com.foxtel.atv", "foxtel"},   // Foxtel Go
        {"au.net.abc.iview", "iview"},   // ABC iview
        {"au.com.kayosports.tv", "kayo"},   // Kayo
        {"com.netflix.ninja", "netflix"},   // Netflix
        {"com.cbs.ca", "paramount"},   // Paramount+
        {"com.amazon.amazonvideo.livingroom", "prime"},   // Prime Video
        {"it.rainet.raiplay", "raiplay"},   // RaiPlay
        {"com.sbs.ondemand.tv", "sbs"},   // SBS On Demand
        {"com.spotify.tv.android", "spotify"},   // Spotify
        {"au.com.stan.and", "stan"},   // Stan
        {"com.stremio.one", "stremio"},   // Stremio
        {"tv.twitch.android.app", "twitch"},   // Twitch
        {"be.vrt.vrtmax", "vrtmax"},   // VRT MAX
        {"be.vtmgo.app", "vtmgo"},   // VTM GO
        {"com.google.android.youtube.tv", "youtube"},   // YouTube
        {"com.amazon.firetv.youtube", "youtube"},   // YouTube
        {"com.wbd.stream", "max"},   // HBO Max
    };

    /** the face of an installed app's package, or null (then TileArt draws the app's own icon) */
    static String face(String pkg) {
        if (pkg == null) return null;
        for (String[] f : FACES) if (f[0].equals(pkg)) return f[1];
        return null;
    }

    static String asset(String face) { return "couch-faces/" + face + ".png"; }
}
