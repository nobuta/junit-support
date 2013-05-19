package suport.async;

import java.util.ArrayList;
import java.util.List;

import org.hamcrest.Matcher;

public class AsyncTestSupport {
	
	private class NotificationTrace<T> {
		private final Object traceLock = new Object();
		private final List<T> trace = new ArrayList<T>();
		private long timeoutMs;
		
		private NotificationTrace(long timeoutMs) {
			this.timeoutMs = timeoutMs;
		}
		
		public void append(T message) {
			synchronized (traceLock) {
				trace.add(message);
				traceLock.notifyAll();
			}
		}
		
		public void containsNotification(Matcher<? super T> criteria) throws InterruptedException {
			Timeout timeout = new Timeout(timeoutMs);
			
			synchronized (traceLock) {
				NotificationStream<T> stream = new NotificationStream<T>(trace, criteria);
				while (! stream.hasMatched()) {
					if (timeout.hasTimeout()) throw new AssertionError(failureDescriptionFrom(criteria));
					else timeout.waitOn(traceLock); // appendされるまで待機
				}
			}
		}
		
		private String failureDescriptionFrom(Matcher<? super T> criteria) {
			// TODO: traceやcriteriaから詳細な情報を作てもよい
			return "Timeout"; 
		}
	
		private class NotificationStream<N> {
			private final List<N> notification;
			private final Matcher<? super N> criteria;
			private int next = 0;
			
			public NotificationStream(List<N> notification, Matcher<? super N> criteria) {
				this.notification = notification;
				this.criteria = criteria;
			}
			
			public boolean hasMatched() {
				while (next < notification.size()) { // notificationのサイズが変更(新しい通知がある)されていたら
					N notificationItem = notification.get(next);
					
					if (criteria.matches(notificationItem)) {
						return true;
					}
					// 次の通知を見る
					next++;
				}
				return false;
			}
		}
		
		private class Timeout {
			private final long endTime;
			
			/**
			 * @param duration　待機時間
			 */
			public Timeout(long duration) {
				this.endTime = System.currentTimeMillis() + duration;
			}
			
			public boolean hasTimeout() {
				return timeRemaining() <= 0;
			}
			
			public void waitOn(Object lock) throws InterruptedException {
				long waitTime = timeRemaining();
				if (waitTime > 0) lock.wait(waitTime);
			}
			
			/**
			 * @return 残り待機時間
			 */
			private long timeRemaining() {
				return endTime - System.currentTimeMillis();
			}
		}
	}
}