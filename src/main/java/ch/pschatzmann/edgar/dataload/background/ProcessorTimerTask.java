package ch.pschatzmann.edgar.dataload.background;

import java.util.TimerTask;

import org.apache.log4j.Logger;

/**
 * Timer task which supports the scheduling of classes which implement the IProcess interface
 * 
 * @author pschatzmann
 *
 */
public class ProcessorTimerTask extends TimerTask {
	private IProcess main;
	private static final Logger LOG = Logger.getLogger(ProcessorTimerTask.class);
	private static boolean isActive=false;
	
	public ProcessorTimerTask(IProcess mainLoadByCompany) {
		main = mainLoadByCompany;
	}

	@Override
	public void run() {
		try {
			if (!isActive) {
				isActive = true;
				LOG.info("START of update");
				main.process();
				LOG.info("END of update");
				isActive = false;
			} else {
				LOG.info("Update not started because there is still an update running...");
			}
		} catch (Exception e) {
			isActive = false;
			LOG.error(e, e);
		}
	}

}