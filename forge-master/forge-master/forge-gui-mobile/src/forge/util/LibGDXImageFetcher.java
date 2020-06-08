package forge.util;

import com.badlogic.gdx.files.FileHandle;
import forge.Forge;
import forge.GuiBase;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class LibGDXImageFetcher extends ImageFetcher {
    @Override
    protected Runnable getDownloadTask(String[] downloadUrls, String destPath, Runnable notifyObservers) {
        return new LibGDXDownloadTask(downloadUrls, destPath, notifyObservers);
    }

    private static class LibGDXDownloadTask implements Runnable {
        private final String[] downloadUrls;
        private final String destPath;
        private final Runnable notifyObservers;

        LibGDXDownloadTask(String[] downloadUrls, String destPath, Runnable notifyObservers) {
            this.downloadUrls = downloadUrls;
            this.destPath = destPath;
            this.notifyObservers = notifyObservers;
        }

        private void doFetch(String urlToDownload) throws IOException {
            URL url = new URL(urlToDownload);
            System.out.println("Attempting to fetch: " + url);
            java.net.URLConnection c = url.openConnection();
            c.setRequestProperty("User-Agent", "");

            InputStream is = c.getInputStream();
            // First, save to a temporary file so that nothing tries to read
            // a partial download.
            FileHandle destFile = new FileHandle(destPath + ".tmp");
            System.out.println(destPath);
            destFile.parent().mkdirs();

            // Conversion to JPEG will be handled differently depending on the platform
            Forge.getDeviceAdapter().convertToJPEG(is, new FileOutputStream(destFile.file()));
            destFile.moveTo(new FileHandle(destPath));

            System.out.println("Saved image to " + destPath);
            GuiBase.getInterface().invokeInEdtLater(notifyObservers);
        }

        public void run() {
            for (String urlToDownload : downloadUrls) {
                try {
                    doFetch(urlToDownload);
                    break;
                } catch (IOException e) {
                    System.out.println("Failed to download card [" + destPath + "] image: " + e.getMessage());
                }
            }
        }
    }

}
