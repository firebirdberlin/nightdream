/*
 * NightDream
 * Copyright (C) 2025 Stefan Fruhner
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.firebirdberlin.nightdream.ui.background;

import android.graphics.Bitmap;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;

import java.io.BufferedInputStream;
import java.io.InputStream;

public class GifMovie extends AnimationDrawable {
    private final int height;
    private final int width;
    private boolean decoded;
    private GifDecoder mGifDecoder;
    private Bitmap mTmpBitmap;

    public GifMovie(InputStream is) {
        this(is, false);
    }

    public GifMovie(InputStream is, boolean inline) {
        super();
        InputStream bis = is;
        if (!(bis instanceof BufferedInputStream)) bis = new BufferedInputStream(is, 32768);
        decoded = false;
        mGifDecoder = new GifDecoder();
        mGifDecoder.read(bis);
        mTmpBitmap = mGifDecoder.getFrame(0);
        height = mTmpBitmap.getHeight();
        width = mTmpBitmap.getWidth();
        addFrame(new BitmapDrawable(mTmpBitmap), mGifDecoder.getDelay(0));
        setOneShot(mGifDecoder.getLoopCount() != 0);
        setVisible(true, true);
        Runnable loader = () -> {
            mGifDecoder.complete();
            int i, n = mGifDecoder.getFrameCount(), t;
            for (i = 1; i < n; i++) {
                mTmpBitmap = mGifDecoder.getFrame(i);
                t = mGifDecoder.getDelay(i);
                addFrame(new BitmapDrawable(mTmpBitmap), t);
            }
            decoded = true;
            mGifDecoder = null;
        };
        if (inline) {
            loader.run();
        } else {
            new Thread(loader).start();
        }
    }

    public boolean isDecoded() {
        return decoded;
    }

    public int getMinimumHeight() {
        return height;
    }

    public int getMinimumWidth() {
        return width;
    }

    public int getIntrinsicHeight() {
        return height;
    }

    public int getIntrinsicWidth() {
        return width;
    }
}
