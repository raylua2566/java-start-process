package de.mxro.process.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class StreamReader {

	private final class WorkerThread extends Thread {
		private final StreamListener listener;
		private final InputStream stream;

		private transient int timeout;

		private WorkerThread(final StreamListener listener,
				final InputStream stream) {
			this.listener = listener;
			this.stream = stream;
			this.timeout = 100;
		}

		@Override
		public void run() {
			final BufferedReader reader = new BufferedReader(
					new InputStreamReader(stream));
			try {
				String read;

				while (true) {
					while (!reader.ready()) {
						if (stop) {
							stopReader();
							return;
						}
						waitForInput();
					}

					if (stop) {
						stopReader();
						return;
					}

					read = reader.readLine();

					if (read != null) {
						listener.onOutputLine(read);
					}
					waitForInput();
				}

			} catch (final IOException e) {
				listener.onError(e);
			}

		}

		/**
		 * Wait longer and longer to not keep CPU busy.
		 */
		private final void waitForInput() {
			try {
				Thread.sleep(this.timeout);
			} catch (final InterruptedException e) {
				throw new RuntimeException();
			}

			if (this.timeout < 2000) {
				this.timeout = this.timeout + 500;
			}

		}

		private void stopReader() throws IOException {
			stream.close();
			stopped = true;
			listener.onClosed();
		}
	}

	private transient int timeout;
	private final Thread t;
	private volatile boolean stop = false;
	private volatile boolean stopped = false;

	public void stop() {

		if (stopped) {
			return;
		}
		stop = true;
		while (!stopped) {
			Thread.yield();
		}
	}

	public StreamReader(final InputStream stream, final StreamListener listener) {
		super();
		this.t = new WorkerThread(listener, stream);
		this.t.start();
	}

}
