package org.videolan.medialibrary;

import org.junit.Test;
import org.videolan.medialibrary.interfaces.Medialibrary;
import org.videolan.medialibrary.stubs.StubMedialibrary;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

public class MedialibraryCallbackTest {

    @Test
    public void mediaCallbackMayUnregisterDuringDispatch() {
        final StubMedialibrary medialibrary = new StubMedialibrary();
        final AtomicInteger notifications = new AtomicInteger();
        final Medialibrary.MediaCb callback = new Medialibrary.MediaCb() {
            @Override
            public void onMediaAdded() {
            }

            @Override
            public void onMediaModified() {
                notifications.incrementAndGet();
                medialibrary.removeMediaCb(this);
            }

            @Override
            public void onMediaDeleted(long[] id) {
            }

            @Override
            public void onMediaConvertedToExternal(long[] id) {
            }
        };

        medialibrary.addMediaCb(callback);
        medialibrary.onMediaUpdated();
        medialibrary.onMediaUpdated();

        assertEquals("A callback removed during dispatch must not crash or be called again", 1, notifications.get());
    }
}
