package com.jeffthefate.podcatcher;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

public final class PluginBundleManagerTap
{
    /**
     * Method to verify the content of the bundle are correct.
     * <p>
     * This method will not mutate {@code bundle}.
     *
     * @param bundle bundle to verify. May be null, which will always return false.
     * @return true if the Bundle is valid, false if the bundle is invalid.
     */
    public static boolean isBundleValid(final Bundle bundle)
    {
        if (null == bundle)
            return false;

        /*
         * Make sure the expected extras exist
         */
        if (!bundle.containsKey(Constants.BUNDLE_EXTRA_BOOLEAN_TAP)) {
            Log.e(Constants.LOG_TAG,
                  String.format("bundle must contain extra %s",
                          Constants.BUNDLE_EXTRA_BOOLEAN_TAP)); //$NON-NLS-1$
            return false;
        }
        if (!bundle.containsKey(Constants.BUNDLE_EXTRA_INT_VERSION_CODE)) {
            Log.e(Constants.LOG_TAG,
                  String.format("bundle must contain extra %s",
                          Constants.BUNDLE_EXTRA_INT_VERSION_CODE)); //$NON-NLS-1$
            return false;
        }

        /*
         * Make sure the correct number of extras exist. Run this test after checking for specific Bundle
         * extras above so that the error message is more useful. (E.g. the caller will see what extras are
         * missing, rather than just a message that there is the wrong number).
         */
        if (2 != bundle.keySet().size()) {
            Log.e(Constants.LOG_TAG,
                  String.format("bundle must contain 2 keys, but currently " +
                          "contains %d keys: %s", bundle.keySet().size(),
                          bundle.keySet())); //$NON-NLS-1$
            return false;
        }

        if (bundle.getBoolean(Constants.BUNDLE_EXTRA_BOOLEAN_TAP, false) !=
            bundle.getBoolean(Constants.BUNDLE_EXTRA_BOOLEAN_TAP, true)) {
            Log.e(Constants.LOG_TAG,
                    String.format("bundle extra %s appears to be the wrong " +
                            "type.  It must be a boolean",
                            Constants.BUNDLE_EXTRA_BOOLEAN_TAP));
            return false;
        }

        if (bundle.getInt(Constants.BUNDLE_EXTRA_INT_VERSION_CODE, 0) !=
            bundle.getInt(Constants.BUNDLE_EXTRA_INT_VERSION_CODE, 1)) {
            Log.e(Constants.LOG_TAG,
                  String.format("bundle extra %s appears to be the wrong " +
                          "type.  It must be an int",
                          Constants.BUNDLE_EXTRA_INT_VERSION_CODE));
            return false;
        }

        return true;
    }

    /**
     * @param context Application context.
     * @param message The toast message to be displayed by the plug-in. Cannot be null.
     * @return A plug-in bundle.
     */
    public static Bundle generateBundle(final Context context,
            boolean mode, boolean edit, int value) {
        Bundle result = new Bundle();
        result.putInt(Constants.BUNDLE_EXTRA_INT_VERSION_CODE,
                Constants.getVersionCode(context));
        result.putBoolean(Constants.BUNDLE_EXTRA_BOOLEAN_TAP, mode);
        result.putBoolean(Constants.BUNDLE_EXTRA_BOOLEAN_SENSITIVITY, edit);
        result.putInt(Constants.BUNDLE_EXTRA_INT_SENSITIVITY, value);

        return result;
    }

    /**
     * Private constructor prevents instantiation
     *
     * @throws UnsupportedOperationException because this class cannot be instantiated.
     */
    private PluginBundleManagerTap() {
        throw new UnsupportedOperationException("This class is non-instantiable"); //$NON-NLS-1$
    }
}