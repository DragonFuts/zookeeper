package zookeeper;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import com.github.zkclient.IZkDataListener;
import com.github.zkclient.ZkClient;

/**
 * 防止一删除节点就会通知所有的监听者，但只有一个人能拿到锁，羊群效应
 * 
 * @author Dragon
 *
 */
public class ZKLockSequence implements Lock {

	private static ZkClient zkClient = new ZkClient("127.0.0.1:2181");

	private static String path = "/lock";

	private ThreadLocal<String> curNodeLocal = new ThreadLocal<String>();

	static {
		if (!zkClient.exists(path)) {
			synchronized (path) {
				if (!zkClient.exists(path)) {
					zkClient.createPersistent(path);

				}
			}
		}
	}

	public void lock() {
		final Object obj = new Object();

		String curNode = zkClient.createEphemeralSequential(path + "/", null);
		curNodeLocal.set(curNode);
		
		List<String> children = zkClient.getChildren(path);
		Collections.sort(children);

		int curIndex = Collections.binarySearch(children, curNode.substring(curNode.lastIndexOf("/") + 1));
		// 假如不存在或者我是第一个，则不用等待
		if(curIndex == -1 || curIndex == 0) {
			return;
		}
		
		// 找到我之前的节点,监听他的删除事件
		String before = children.get(curIndex - 1);
		// 监听锁释放
		IZkDataListener listener = new IZkDataListener() {

			public void handleDataDeleted(String dataPath) throws Exception {
				System.out.println(dataPath + "删除了");
				synchronized (obj) {
					obj.notify();
				}
			}

			public void handleDataChange(String dataPath, byte[] data) throws Exception {
			}
		};
		zkClient.subscribeDataChanges(path + "/" + before, listener);

		// 等待前一个人删除锁
		synchronized (obj) {
			try {
				obj.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.out.println(curNode + "拿到了锁");
	}

	public void unlock() {
		zkClient.delete(curNodeLocal.get());
	}

	public void lockInterruptibly() throws InterruptedException {
		// TODO Auto-generated method stub

	}

	public boolean tryLock() {
		return false;
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
