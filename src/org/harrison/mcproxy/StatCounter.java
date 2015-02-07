package org.harrison.mcproxy;

public class StatCounter
{
	private static final int SNAPSHOT_LENGTH_MS = 1000;
	
	public static class StatRec
	{
		private long value;
		private long count;
		private long startms;
		private long endms;
		private String valueLabel;
		private String countLabel;
		
		public StatRec(String valueLabel, String countLabel)
		{
			this.valueLabel = valueLabel;
			this.countLabel = countLabel;
			
			startms = System.currentTimeMillis();
		}
		
		void bump(long delta, long now)
		{
			value += delta;
			count++;
			endms = now;
		}
		
		void reset(long now)
		{
			value = 0;
			count = 0;
			startms = now;
			endms = now;
		}
		
		long getElapsed()
		{
			return endms - startms;
		}
		
		public long getValue()
		{
			return value;
		}
		
		public long getCount()
		{
			return count;
		}
		
		public String toString()
		{
			long elapsed = endms - startms;
			if ( elapsed == 0 ) elapsed = 1;
			float val_ps = (value*1000) / (float)elapsed;
			float count_ps = (count*1000) / (float)elapsed;
			
			return String.format("%2.0f%s, %2.0f%s", val_ps, valueLabel, count_ps, countLabel);
		}
	}
	
	private StatRec totalStat;
	private StatRec lastSnapshot;
	private StatRec curSnapshot;
	private long snapshotNum;
	
	public StatCounter(String valueLabel, String countLabel)
	{
		totalStat = new StatRec(valueLabel, countLabel);
		curSnapshot = new StatRec(valueLabel, countLabel);
	}
	
	public StatRec getTotal()
	{
		return totalStat;
	}
	
	public StatRec getSnapshot()
	{
		return lastSnapshot;
	}
	
	public void bump(long delta)
	{
		long now = System.currentTimeMillis();
		
		totalStat.bump(delta, now);
		curSnapshot.bump(delta, now);
		
		if ( curSnapshot.getElapsed() > SNAPSHOT_LENGTH_MS )
		{
			lastSnapshot = curSnapshot;
			curSnapshot = new StatRec(lastSnapshot.valueLabel, lastSnapshot.countLabel);
			snapshotNum++;
		}
	}
	
	public long getSnapshotNum()
	{
		return snapshotNum;
	}
	
	public String toString()
	{
		String s = "";
		if ( lastSnapshot != null )
		{
			s += lastSnapshot.toString() + ", ";
		}
		s += "total: " + totalStat.toString();
		
		return s;
	}
}
