package android.print;

import android.os.Build;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.IOException;
public class PdfPrinterHelper {
  public interface ExportCallback {
    void onSuccess(@NonNull String filePath);
    void onError(@NonNull String code, @Nullable String message);
  }

  public static void exportPdf(
    @NonNull PrintAttributes attributes,
    @NonNull PrintDocumentAdapter adapter,
    @NonNull String targetDirectory,
    @NonNull String targetName,
    @NonNull ExportCallback callback
  ) {
    adapter.onLayout(
      null,
      attributes,
      null,
      new PrintDocumentAdapter.LayoutResultCallback() {
        @Override
        public void onLayoutFinished(PrintDocumentInfo info, boolean changed) {
          try {
            File dir = new File(targetDirectory);
            dir.mkdirs(); //noinspection ResultOfMethodCallIgnored
            File outputFile = new File(dir, targetName + ".pdf");

            ParcelFileDescriptor pfd = ParcelFileDescriptor.open(
              outputFile,
              ParcelFileDescriptor.MODE_READ_WRITE
                | ParcelFileDescriptor.MODE_CREATE
                | ParcelFileDescriptor.MODE_TRUNCATE
            );

            adapter.onWrite(
              new PageRange[]{PageRange.ALL_PAGES},
              pfd,
              new CancellationSignal(),
              new PrintDocumentAdapter.WriteResultCallback() {
                @Override
                public void onWriteFinished(PageRange[] pages) {
                  try { pfd.close(); } catch (IOException ignored) {}
                  adapter.onFinish();
                  if (pages == null || pages.length == 0) {
                    callback.onError("ERR_WRITE_FAILED", "Unable to convert html to pdf document!");
                    return;
                  }

                  callback.onSuccess(outputFile.getAbsolutePath());
                }

                @Override
                public void onWriteFailed(CharSequence error) {
                  try { pfd.close(); } catch (IOException ignored) {}
                  adapter.onFinish();
                  callback.onError("ERR_WRITE_FAILED", error.toString());
                }

                @Override
                public void onWriteCancelled() {
                  try { pfd.close(); } catch (IOException ignored) {}
                  adapter.onFinish();
                  callback.onError("ERR_WRITE_CANCELLED", "Write cancelled");
                }
              }
            );
          } catch (Exception e) {
            adapter.onFinish();
            callback.onError("ERR_FILE_ERROR", e.getMessage());
          }
        }

        @Override
        public void onLayoutFailed(CharSequence error) {
          adapter.onFinish();
          callback.onError("ERR_LAYOUT_FAILED", error.toString());
        }

        @Override
        public void onLayoutCancelled() {
          adapter.onFinish();
          callback.onError("ERR_LAYOUT_CANCELLED", "Layout cancelled");
        }
      },
      null
    );
  }
}
