package zookeeper;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class OrderGenerator {

	private static int seq = 5;

	private static int size = 10;

	public static int generareOrderNo() {
		return seq ++ ;
	}
	
	private static Lock lock = null;
	
	private static CountDownLatch latch = new CountDownLatch(size);
	
	static {
		lock = new ReentrantLock();
		lock = new ZKLock();
		lock = new ZKLockSequence();
	}

	public static void main(String[] args) throws Exception {
		
		for (int i = 0; i < size; i++) {
			new Thread(new Runnable() {
				
				public void run() {
					latch.countDown();
					try {
						latch.await();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					lock.lock();
					int orderNo = OrderGenerator.generareOrderNo();
					System.out.println(String.format("线程%s获得订单号：%s", Thread.currentThread().getName(), orderNo));
					lock.unlock();
//					if(!set.add(orderNo)) {
//						System.out.println("已经存在:" + orderNo);
//					}
				}
			}).start();
		}
		System.in.read();
	}

}
