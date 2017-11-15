package com.jinosh.signaturepad;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import java.io.ByteArrayOutputStream;

/**This class consist of all utils related to {@Link in.hexagon.signaturepad.SignatureView}
 *
 * Created by administrator on 24/7/17.
 */

public class BitmapUtils {

    /**
     * This method convert base64 encoded value into string
     *
     * @param encodedString
     * @return bitmap
     */
    public static Bitmap stringToBitMap(String encodedString) {
        try {
            byte[] encodeByte = Base64.decode(encodedString, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(encodeByte, 0,
                    encodeByte.length);
            return bitmap;
        } catch (Exception e) {
            e.getMessage();
            return null;
        }
    }

    /**
     * This method convert bitmap to base64 encoded string.
     *
     * @param bitmap
     * @return
     */
    public static String bitMapToString(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] b = baos.toByteArray();
        String temp = Base64.encodeToString(b, Base64.DEFAULT);
        return temp;
    }
}
