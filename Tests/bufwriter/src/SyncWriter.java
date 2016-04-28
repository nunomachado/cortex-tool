package bufwriter;

/**
 * Title:
 * Description:
 * Copyright:    Copyright (c) 2003
 * Company:
 * @author
 * @version 1.0
 */

public class SyncWriter implements Runnable {
	private int _writeVal;
	public int _writeCount;
	private static Buffer _buf;

	public SyncWriter(Buffer buf,int val) {
		_writeVal = val;
		_buf = buf;
	}

	public void run()
	{
		int i = 0;
		//System.out.println("Sync writer run");
		while (i<10)
		{
			synchronized (_buf)
			{
				if (_buf._pos >= _buf._size) continue;
				_buf._array[_buf._pos] = _writeVal;
				_buf._pos += 1;
				_buf._count += 1;
			}
			i++;
		}
	}

	public int getCounter ()
	{
		return _writeCount;
	}
}