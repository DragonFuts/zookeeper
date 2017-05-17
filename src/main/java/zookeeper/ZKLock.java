package zookeeper;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import com.github.zkclient.IZkDataListener;
import com.github.zkclient.ZkClient;
import com.github.zkclient.exception.ZkNodeExistsException;

/**
 * 有羊群效应的问题：一删除节点就会通知所有的监听者，但只有一个人能拿到锁，
 * @author Dragon
 *
 */
public class ZKLock implements Lock {

	private static ZkClient zkClient = new ZkClient("127.0.0.1:2181");

	private static String path = "/lock";
	
	private static Object obj = new Object();
	
	static {
		//监听锁释放
		IZkDataListener listener = new IZkDataListener() {
			
			public void handleDataDeleted(String dataPath) throws Exception {
				synchronized (obj) {
					obj.notify();
				}
			}
			
			public void handleDataChange(String dataPath, byte[] data) throws Exception {
			}
		};
		zkClient.subscribeDataChanges(path, listener);
	}

	public void lock() {
		while(!tryLock()) {
			synchronized (obj) {
				try {
					obj.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public void unlock() {
		zkClient.delete(path);
	}

	public void lockInterruptibly() throws InterruptedException {

	}

	public boolean tryLock() {
		try {
			zkClient.createEphemeral(path);
			return true;
		} catch (ZkNodeExistsException e) {
			return false;
		}
	}

	public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
		// TODO Auto-generated method stub
		return false;
	}

	public Condition newCondition() {
		// TODO Auto-generated method stub
		return null;
	}

}
