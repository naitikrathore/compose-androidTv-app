package android.media.tv;


import android.content.ContentUris;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import android.media.tv.ContentRatingSystem.*;

import com.iwedia.cltv.platform.model.Constants;

/**
 * Parses Content Ratings
 */
public class ContentRatingsParser {
    private static final String TAG = "ContentRatingsParser";
    private static final boolean DEBUG = false;

    public static final String DOMAIN_SYSTEM_RATINGS = "com.android.tv";

    private static final String TAG_RATING_SYSTEM_DEFINITIONS = "rating-system-definitions";
    private static final String TAG_RATING_SYSTEM_DEFINITION = "rating-system-definition";
    private static final String TAG_SUB_RATING_DEFINITION = "sub-rating-definition";
    private static final String TAG_RATING_DEFINITION = "rating" +
            "-definition";
    private static final String TAG_SUB_RATING = "sub-rating";
    private static final String TAG_RATING = "rating";
    private static final String TAG_RATING_ORDER = "rating-order";

    private static final String ATTR_VERSION_CODE = "versionCode";
    private static final String ATTR_NAME = "name";
    private static final String ATTR_TITLE = "title";
    private static final String ATTR_VALUE = "value";
    private static final String ATTR_COUNTRY = "country";
    private static final String ATTR_ICON = "icon";
    private static final String ATTR_DESCRIPTION = "description";
    private static final String ATTR_CONTENT_AGE_HINT = "contentAgeHint";
    private static final String VERSION_CODE = "1";

    private final Context mContext;
    private Resources mResources;
    private String mXmlVersionCode;
    private  static List<ContentRatingSystem> mRatingSystems = null;

    public ContentRatingsParser(Context context) {
        mContext = context;

    }

    public void clearRatingSystem() {
        mRatingSystems = null;
    }

    public List<ContentRatingSystem> parse(TvContentRatingSystemInfo info) {
        List<ContentRatingSystem> ratingSystems = null;
        Uri uri = info.getXmlUri();
        if (DEBUG) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "Parsing rating system for " + uri);
        }
        try {
            String packageName = uri.getAuthority();
            String curPackageName=mContext.getPackageName();
            //MtkLog.d(Constants.LogTag.CLTV_TAG + TAG, " packageName="+packageName +",curPackageName="+curPackageName);
            if(TextUtils.equals(packageName,curPackageName) && mRatingSystems != null){
                //MtkLog.d(Constants.LogTag.CLTV_TAG + TAG, "return instance mRatingSystems");
                return mRatingSystems;
            }

            if(!TextUtils.equals(packageName,curPackageName)&&!TextUtils.equals(packageName,"com.android.cts.verifier")){
                return null;
            }
            int resId = (int) ContentUris.parseId(uri);
            try (XmlResourceParser parser =
                         mContext.getPackageManager().getXml(packageName, resId, null)) {

                if (parser == null) {
                    throw new IllegalArgumentException("Cannot get XML with URI " + uri);
                }
                ratingSystems = parse(parser, packageName, !info.isSystemDefined());
            }

            if(TextUtils.equals(packageName,curPackageName)){
                mRatingSystems = ratingSystems;
            }
        } catch (Exception e) {
            // Catching all exceptions and print which URI is malformed XML with description
            // and stack trace here.
            // TODO: We may want to print message to stdout.
            Log.w(TAG, "Error parsing XML " + uri, e);
        }
        return ratingSystems;
    }

    private List<ContentRatingSystem> parse(
            XmlResourceParser parser, String domain, boolean isCustom)
            throws XmlPullParserException, IOException {
        try {
            mResources = mContext.getPackageManager().getResourcesForApplication(domain);
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Failed to get resources for " + domain, e);
            mResources = mContext.getResources();
        }
        // TODO: find another way to replace the domain the content rating systems defined in TV.
        // Live TV app provides public content rating systems. Therefore, the domain of
        // the content rating systems defined in TV app should be com.android.tv instead of
        // this app's package name.
        if (domain.equals(mContext.getPackageName())) {
            domain = DOMAIN_SYSTEM_RATINGS;
        }

        // Consume all START_DOCUMENT which can appear more than once.
        while (parser.next() == XmlPullParser.START_DOCUMENT) {
        }

        int eventType = parser.getEventType();
        assertEquals(eventType, XmlPullParser.START_TAG, "Malformed XML: Not a valid XML file");
        assertEquals(
                parser.getName(),
                TAG_RATING_SYSTEM_DEFINITIONS,
                "Malformed XML: Should start with tag " + TAG_RATING_SYSTEM_DEFINITIONS);

        boolean hasVersionAttr = false;
        for (int i = 0; i < parser.getAttributeCount(); i++) {
            String attr = parser.getAttributeName(i);
            if (ATTR_VERSION_CODE.equals(attr)) {
                hasVersionAttr = true;
                mXmlVersionCode = parser.getAttributeValue(i);
            }
        }
        if (!hasVersionAttr) {
            throw new XmlPullParserException(
                    "Malformed XML: Should contains a version attribute"
                            + " in "
                            + TAG_RATING_SYSTEM_DEFINITIONS);
        }

        List<ContentRatingSystem> ratingSystems = new ArrayList<>();
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            switch (parser.getEventType()) {
                case XmlPullParser.START_TAG:
                    if (TAG_RATING_SYSTEM_DEFINITION.equals(parser.getName())) {
                        ratingSystems.add(parseRatingSystemDefinition(parser, domain, isCustom));
                    } else {
                        checkVersion(
                                "Malformed XML: Should contains " + TAG_RATING_SYSTEM_DEFINITION);
                    }
                    break;
                case XmlPullParser.END_TAG:
                    if (TAG_RATING_SYSTEM_DEFINITIONS.equals(parser.getName())) {
                        eventType = parser.next();
                        assertEquals(
                                eventType,
                                XmlPullParser.END_DOCUMENT,
                                "Malformed XML: Should end with tag "
                                        + TAG_RATING_SYSTEM_DEFINITIONS);
                        return ratingSystems;
                    } else {
                        checkVersion(
                                "Malformed XML: Should end with tag "
                                        + TAG_RATING_SYSTEM_DEFINITIONS);
                    }
                    break;
                default:
                    break;
            }
        }
        throw new XmlPullParserException(
                TAG_RATING_SYSTEM_DEFINITIONS
                        + " section is incomplete or section ending tag is missing");
    }

    private static void assertEquals(int a, int b, String msg) throws XmlPullParserException {
        if (a != b) {
            throw new XmlPullParserException(msg);
        }
    }

    private static void assertEquals(String a, String b, String msg) throws XmlPullParserException {
        if (!b.equals(a)) {
            throw new XmlPullParserException(msg);
        }
    }

    private void checkVersion(String msg) throws XmlPullParserException {
        if (!VERSION_CODE.equals(mXmlVersionCode)) {
            throw new XmlPullParserException(msg);
        }
    }

    private ContentRatingSystem parseRatingSystemDefinition(
            XmlResourceParser parser, String domain, boolean isCustom)
            throws XmlPullParserException, IOException {
        Builder builder = new Builder(mContext);

        builder.setDomain(domain);
        for (int i = 0; i < parser.getAttributeCount(); i++) {
            String attr = parser.getAttributeName(i);
            switch (attr) {
                case ATTR_NAME:
                    builder.setName(parser.getAttributeValue(i));
                    break;
                case ATTR_COUNTRY:
                    for (String country : parser.getAttributeValue(i).split("\\s*,\\s*")) {
                        builder.addCountry(country);
                    }
                    break;
                case ATTR_TITLE:
                    builder.setTitle(getTitle(parser, i));
                    break;
                case ATTR_DESCRIPTION:
                    builder.setDescription(
                            mResources.getString(parser.getAttributeResourceValue(i, 0)));
                    break;
                default:
                    checkVersion(
                            "Malformed XML: Unknown attribute "
                                    + attr
                                    + " in "
                                    + TAG_RATING_SYSTEM_DEFINITION);
                    break;
            }
        }

        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            int eventType = parser.getEventType();
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    String tag = parser.getName();
                    switch (tag) {
                        case TAG_RATING_DEFINITION:
                            builder.addRatingBuilder(parseRatingDefinition(parser));
                            break;
                        case TAG_SUB_RATING_DEFINITION:
                            builder.addSubRatingBuilder(parseSubRatingDefinition(parser));
                            break;
                        case TAG_RATING_ORDER:
                            builder.addOrderBuilder(parseOrder(parser));
                            break;
                        default:
                            checkVersion(
                                    "Malformed XML: Unknown tag "
                                            + tag
                                            + " in "
                                            + TAG_RATING_SYSTEM_DEFINITION);
                            break;
                    }
                    break;
                case XmlPullParser.END_TAG:
                    if (TAG_RATING_SYSTEM_DEFINITION.equals(parser.getName())) {
                        builder.setIsCustom(isCustom);
                        return builder.build();
                    } else {
                        checkVersion(
                                "Malformed XML: Tag mismatch for " + TAG_RATING_SYSTEM_DEFINITION);
                    }
                    break;
                default:
                    checkVersion(
                            "Malformed XML: Unknown event type "
                                    + eventType
                                    + " in "
                                    + TAG_RATING_SYSTEM_DEFINITION);
                    break;
            }
        }
        throw new XmlPullParserException(
                TAG_RATING_SYSTEM_DEFINITION
                        + " section is incomplete or section ending tag is missing");
    }

    private Rating.Builder parseRatingDefinition(XmlResourceParser parser)
            throws XmlPullParserException, IOException {
        Rating.Builder builder = new Rating.Builder();

        for (int i = 0; i < parser.getAttributeCount(); i++) {
            String attr = parser.getAttributeName(i);
            switch (attr) {
                case ATTR_NAME:
                    builder.setName(parser.getAttributeValue(i));
                    break;
                case ATTR_TITLE:
                    builder.setTitle(getTitle(parser, i));
                    break;
                case ATTR_VALUE:
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "parser add value : " + getTitle(parser, i));
                    //builder.setValue(getTitle(parser, i));
                    break;
                case ATTR_DESCRIPTION:
                    String des = mResources.getString(parser.getAttributeResourceValue(i, 0));
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "DESCRIPTION" + des.substring(0, 2));
                    int rat = -1;
                    if (des != null) {
                        if ("abc".equals(des.substring(0, 3))) {
                            rat = Integer.parseInt(des.substring(3));
                            Log.d(Constants.LogTag.CLTV_TAG + TAG, "Change rating translation" + rat);
                           // des = mResources.getString(R.string.description_age_num,rat);
                        }else if ("cba".equals(des.substring(0, 3))) {
                            rat = Integer.parseInt(des.substring(3));
                            Log.d(Constants.LogTag.CLTV_TAG + TAG, "Change rating translation" + rat);
                            //des = mResources.getString(R.string.description_br_tv_num,rat);
                        }
                    }
                    builder.setDescription(des);
                    break;
                case ATTR_ICON:
                    builder.setIcon(
                            mResources.getDrawable(parser.getAttributeResourceValue(i, 0), null));
                    break;
                case ATTR_CONTENT_AGE_HINT:
                    int contentAgeHint = -1;
                    try {
                        contentAgeHint = Integer.parseInt(parser.getAttributeValue(i));
                    } catch (NumberFormatException ignored) {
                    }

                    if (contentAgeHint < 0) {
                        throw new XmlPullParserException(
                                "Malformed XML: "
                                        + ATTR_CONTENT_AGE_HINT
                                        + " should be a non-negative number");
                    }
                    builder.setContentAgeHint(contentAgeHint);
                    break;
                default:
                    checkVersion(
                            "Malformed XML: Unknown attribute "
                                    + attr
                                    + " in "
                                    + TAG_RATING_DEFINITION);
                    break;
            }
        }

        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            switch (parser.getEventType()) {
                case XmlPullParser.START_TAG:
                    if (TAG_SUB_RATING.equals(parser.getName())) {
                        builder = parseSubRating(parser, builder);
                    } else {
                        checkVersion(
                                ("Malformed XML: Only "
                                        + TAG_SUB_RATING
                                        + " is allowed in "
                                        + TAG_RATING_DEFINITION));
                    }
                    break;
                case XmlPullParser.END_TAG:
                    if (TAG_RATING_DEFINITION.equals(parser.getName())) {
                        return builder;
                    } else {
                        checkVersion("Malformed XML: Tag mismatch for " + TAG_RATING_DEFINITION);
                    }
                    break;
                default:
                    break;
            }
        }
        throw new XmlPullParserException(
                TAG_RATING_DEFINITION + " section is incomplete or section ending tag is missing");
    }

    private SubRating.Builder parseSubRatingDefinition(XmlResourceParser parser)
            throws XmlPullParserException, IOException {
        SubRating.Builder builder = new SubRating.Builder();

        for (int i = 0; i < parser.getAttributeCount(); i++) {
            String attr = parser.getAttributeName(i);
            switch (attr) {
                case ATTR_NAME:
                    builder.setName(parser.getAttributeValue(i));
                    break;
                case ATTR_TITLE:
                    builder.setTitle(getTitle(parser, i));
                    break;
                case ATTR_DESCRIPTION:
                    builder.setDescription(
                            mResources.getString(parser.getAttributeResourceValue(i, 0)));
                    break;
                case ATTR_ICON:
                    builder.setIcon(
                            mResources.getDrawable(parser.getAttributeResourceValue(i, 0), null));
                    break;
                default:
                    checkVersion(
                            "Malformed XML: Unknown attribute "
                                    + attr
                                    + " in "
                                    + TAG_SUB_RATING_DEFINITION);
                    break;
            }
        }

        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            switch (parser.getEventType()) {
                case XmlPullParser.END_TAG:
                    if (TAG_SUB_RATING_DEFINITION.equals(parser.getName())) {
                        return builder;
                    } else {
                        checkVersion(
                                "Malformed XML: " + TAG_SUB_RATING_DEFINITION + " isn't closed");
                    }
                    break;
                default:
                    checkVersion("Malformed XML: " + TAG_SUB_RATING_DEFINITION + " has child");
                    break;
            }
        }
        throw new XmlPullParserException(
                TAG_SUB_RATING_DEFINITION
                        + " section is incomplete or section ending tag is missing");
    }

    private Order.Builder parseOrder(XmlResourceParser parser)
            throws XmlPullParserException, IOException {
        Order.Builder builder = new Order.Builder();

        assertEquals(
                parser.getAttributeCount(),
                0,
                "Malformed XML: Attribute isn't allowed in " + TAG_RATING_ORDER);

        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            switch (parser.getEventType()) {
                case XmlPullParser.START_TAG:
                    if (TAG_RATING.equals(parser.getName())) {
                        builder = parseRating(parser, builder);
                    } else {
                        checkVersion(
                                "Malformed XML: Only "
                                        + TAG_RATING
                                        + " is allowed in "
                                        + TAG_RATING_ORDER);
                    }
                    break;
                case XmlPullParser.END_TAG:
                    assertEquals(
                            parser.getName(),
                            TAG_RATING_ORDER,
                            "Malformed XML: Tag mismatch for " + TAG_RATING_ORDER);
                    return builder;
                default:
                    break;
            }
        }
        throw new XmlPullParserException(
                TAG_RATING_ORDER + " section is incomplete or section ending tag is missing");
    }

    private Order.Builder parseRating(XmlResourceParser parser, Order.Builder builder)
            throws XmlPullParserException, IOException {
        for (int i = 0; i < parser.getAttributeCount(); i++) {
            String attr = parser.getAttributeName(i);
            switch (attr) {
                case ATTR_NAME:
                    builder.addRatingName(parser.getAttributeValue(i));
                    break;
                default:
                    checkVersion(
                            "Malformed XML: "
                                    + TAG_RATING_ORDER
                                    + " should only contain "
                                    + ATTR_NAME);
                    break;
            }
        }

        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.getEventType() == XmlPullParser.END_TAG) {
                if (TAG_RATING.equals(parser.getName())) {
                    return builder;
                } else {
                    checkVersion("Malformed XML: " + TAG_RATING + " has child");
                }
            }
        }
        throw new XmlPullParserException(
                TAG_RATING + " section is incomplete or section ending tag is missing");
    }

    private Rating.Builder parseSubRating(XmlResourceParser parser, Rating.Builder builder)
            throws XmlPullParserException, IOException {
        for (int i = 0; i < parser.getAttributeCount(); i++) {
            String attr = parser.getAttributeName(i);
            switch (attr) {
                case ATTR_NAME:
                    builder.addSubRatingName(parser.getAttributeValue(i));
                    break;
                default:
                    checkVersion(
                            "Malformed XML: "
                                    + TAG_SUB_RATING
                                    + " should only contain "
                                    + ATTR_NAME);
                    break;
            }
        }

        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.getEventType() == XmlPullParser.END_TAG) {
                if (TAG_SUB_RATING.equals(parser.getName())) {
                    return builder;
                } else {
                    checkVersion("Malformed XML: " + TAG_SUB_RATING + " has child");
                }
            }
        }
        throw new XmlPullParserException(
                TAG_SUB_RATING + " section is incomplete or section ending tag is missing");
    }

    // Title might be a resource id or a string value. Try loading as an id first, then use the
    // string if that fails.
    private String getTitle(XmlResourceParser parser, int index) {
        int titleResId = parser.getAttributeResourceValue(index, 0);
        if (titleResId != 0) {
            return mResources.getString(titleResId);
        }
        return parser.getAttributeValue(index);
    }
}

