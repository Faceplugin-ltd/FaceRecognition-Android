package com.faceplugin.facerecognitionsdk.ui;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.provider.MediaStore;

import com.faceplugin.facerecognitionsdk.FaceBox;

import java.io.IOException;
import java.io.InputStream;

public class Utils {

    public static Bitmap cropFace(Bitmap src, FaceBox faceBox) {
        if (src == null || src.isRecycled() || faceBox == null) return null;
        int centerX = (faceBox.x1 + faceBox.x2) / 2;
        int centerY = (faceBox.y1 + faceBox.y2) / 2;
        int cropWidth = (int) ((faceBox.x2 - faceBox.x1) * 1.4f);
        if (cropWidth < 2) cropWidth = Math.max(2, Math.max(faceBox.x2 - faceBox.x1, faceBox.y2 - faceBox.y1));

        int cropX1 = Math.max(0, centerX - cropWidth / 2);
        int cropY1 = Math.max(0, centerY - cropWidth / 2);
        int cropX2 = Math.min(src.getWidth() - 1, centerX + cropWidth / 2);
        int cropY2 = Math.min(src.getHeight() - 1, centerY + cropWidth / 2);
        int w = cropX2 - cropX1 + 1;
        int h = cropY2 - cropY1 + 1;
        if (w <= 1 || h <= 1) return null;

        int cropScaleWidth = 200;
        int cropScaleHeight = 200;
        float scaleWidth = ((float) cropScaleWidth) / w;
        float scaleHeight = ((float) cropScaleHeight) / h;

        final Matrix m = new Matrix();
        m.postScale(scaleWidth, scaleHeight);
        try {
            return Bitmap.createBitmap(src, cropX1, cropY1, w, h, m, true);
        } catch (Exception e) {
            return null;
        }
    }

    /** Landmark xy in {@link #cropFace} bitmap pixels (same crop window and 200×200 scale). */
    public static float[] mapLandmarksToCrop(Bitmap src, FaceBox faceBox, int outW, int outH) {
        int n = Math.max(0, Math.min(faceBox.landmarkCount, faceBox.landmarks_68.length / 2));
        float[] out = new float[n * 2];
        if (src == null || src.isRecycled() || n == 0 || outW <= 0 || outH <= 0) return out;
        int centerX = (faceBox.x1 + faceBox.x2) / 2;
        int centerY = (faceBox.y1 + faceBox.y2) / 2;
        int cropWidth = (int) ((faceBox.x2 - faceBox.x1) * 1.4f);
        if (cropWidth < 2) cropWidth = Math.max(2, Math.max(faceBox.x2 - faceBox.x1, faceBox.y2 - faceBox.y1));
        int cropX1 = Math.max(0, centerX - cropWidth / 2);
        int cropY1 = Math.max(0, centerY - cropWidth / 2);
        int cropX2 = Math.min(src.getWidth() - 1, centerX + cropWidth / 2);
        int cropY2 = Math.min(src.getHeight() - 1, centerY + cropWidth / 2);
        float srcW = cropX2 - cropX1 + 1f;
        float srcH = cropY2 - cropY1 + 1f;
        if (srcW < 1f || srcH < 1f) return out;
        float sx = outW / srcW;
        float sy = outH / srcH;
        for (int i = 0; i < n; i++) {
            out[i * 2] = (faceBox.landmarks_68[i * 2] - cropX1) * sx;
            out[i * 2 + 1] = (faceBox.landmarks_68[i * 2 + 1] - cropY1) * sy;
        }
        return out;
    }

    public static int getOrientation(Context context, Uri photoUri) {
        Cursor cursor = context.getContentResolver().query(
                photoUri,
                new String[]{MediaStore.Images.ImageColumns.ORIENTATION},
                null, null, null);
        if (cursor == null) return -1;
        try {
            if (cursor.getCount() != 1) return -1;
            cursor.moveToFirst();
            return cursor.getInt(0);
        } finally {
            cursor.close();
        }
    }

    public static Bitmap getCorrectlyOrientedImage(Context context, Uri photoUri) throws IOException {
        InputStream is = context.getContentResolver().openInputStream(photoUri);
        BitmapFactory.Options dbo = new BitmapFactory.Options();
        dbo.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(is, null, dbo);
        if (is != null) is.close();

        int orientation = getOrientation(context, photoUri);

        is = context.getContentResolver().openInputStream(photoUri);
        Bitmap srcBitmap = BitmapFactory.decodeStream(is);
        if (is != null) is.close();
        if (srcBitmap == null) {
            throw new IOException("Could not decode image");
        }

        if (orientation > 0) {
            Matrix matrix = new Matrix();
            matrix.postRotate(orientation);
            srcBitmap = Bitmap.createBitmap(srcBitmap, 0, 0, srcBitmap.getWidth(), srcBitmap.getHeight(), matrix, true);
        }
        return srcBitmap;
    }
}
