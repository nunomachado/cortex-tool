package pt.tecnico.symbiosis.tloax;
import java.util.HashSet;
import java.util.Set;

import soot.*;

public class XFieldAccessInfo {
	private SootField field;
	private Set writeAccessSet;
	private Set readAccessSet;
	private Boolean isShared;
	XFieldAccessInfo(SootField field)
	{
		this.field = field;
		writeAccessSet = new HashSet<Integer>();
		readAccessSet = new HashSet<Integer>();
		isShared = null;
	}
	public void readAccess(Integer tid)
	{
		readAccessSet.add(tid);
	}
	public void writeAccess(Integer tid)
	{
		writeAccessSet.add(tid);
	}
	public Boolean isFieldShared()
	{
		if(writeAccessSet.size()>1)
		{
			isShared = true;
		}
		else if(writeAccessSet.size()>0&&readAccessSet.size()>1)
		{
			isShared = true;
		}
		else if(readAccessSet.size()>1)
		{
			Type type = field.getType();
			if(type instanceof RefType)
				isShared = true;
			else
				isShared = false;
		}
		else
		{
			isShared = false;
		}
		return isShared;
	}
}
