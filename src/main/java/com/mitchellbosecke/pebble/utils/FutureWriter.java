package com.mitchellbosecke.pebble.utils;

import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class FutureWriter extends Writer {

	private final ConcurrentLinkedQueue<Future<String>> orderedFutures = new ConcurrentLinkedQueue<>();

	private final ExecutorService es;

	private final Writer internalWriter;

	private boolean closed = false;

	public FutureWriter(Writer writer, ExecutorService es) {
		this.internalWriter = writer;
		this.es = es;
	}

	public void enqueue(Future<String> future) throws IOException {
		if (closed) {
			throw new IOException("Writer is closed");
		}
		orderedFutures.add(future);
	}

	@Override
	public void write(final char[] cbuf, final int off, final int len) throws IOException {
		if (orderedFutures.isEmpty()) {
			internalWriter.write(cbuf, off, len);
		} else {
			Future<String> future = es.submit(new Callable<String>() {

				@Override
				public String call() throws Exception {
					char[] chars = new char[len];
					System.arraycopy(cbuf, off, chars, 0, len);
					return new String(chars);
				}

			});

			orderedFutures.add(future);
		}
	}

	@Override
	public void flush() throws IOException {
		for (Future<String> future : orderedFutures) {
			try {
				String result = future.get();
				internalWriter.write(result);
				internalWriter.flush();
			} catch (InterruptedException | ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		orderedFutures.clear();
	}

	@Override
	public void close() throws IOException {
		flush();
		internalWriter.close();
		closed = true;

	}

}
