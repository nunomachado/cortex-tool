package bufwriter;

/**
 * Title:
 * Description:
 * Copyright:    Copyright (c) 2003
 * Company:
 * @author
 * @version 1.0
 */

public class Writer implements Runnable {
	private int _writeVal;
	private int _writeCount;
	private static Buffer _buf;

	public Writer(Buffer buf,int val) {
		_writeVal = val;
		_writeCount = 0;
		_buf = buf;
	}

	public void run()
	{
		int i = 0;
		//System.out.println("Writer run");
		while (i<10)
		{
			if (_buf._pos >= _buf._size) continue;
			_buf._array[_buf._pos] = _writeVal;
			_buf._pos += 1;
			synchronized(_buf)
			{
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