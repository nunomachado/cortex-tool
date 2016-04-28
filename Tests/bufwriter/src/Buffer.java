package bufwriter;

/**
 * Title:
 * Description:
 * Copyright:    Copyright (c) 2003
 * Company:
 * @author
 * @version 1.0
 */
import java.io.*;


public class Buffer {
  public int[] _array;
  public int _pos;
  public int _size;
  public int _count;

  public Buffer(int size) {
    _pos = 0;
    _size = size;
    _array = new int[size];
    _count = 0;
  }

  public void print(DataOutputStream outStream)
  {
    for (int i=0;i<_size;++i)
    {
      try{
        if (i%20 == 0) outStream.writeChars("\n");
        outStream.writeChars(_array[i]+",");
        }
      catch (IOException e) {}
    }
    return;
  }
}