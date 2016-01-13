package bufwriter;

/**
 * Title:
 * Description:
 * Copyright:    Copyright (c) 2003
 * Company:
 * @author
 * @version 1.0
 */

public class Checker implements Runnable{
  private Buffer _buf;
  private int _writtenCount;
  public boolean stop = false;

  public Checker(Buffer buf) {
    _buf = buf;
    _writtenCount = 0;
  }

  public void run()
  {
    while (!stop)
    {
      synchronized (_buf)
      {
         _writtenCount += _buf._pos;
         _buf._pos = 0;
      }
    }
  }

  public int getWrittenCount()
  {
    return _writtenCount;
  }
}