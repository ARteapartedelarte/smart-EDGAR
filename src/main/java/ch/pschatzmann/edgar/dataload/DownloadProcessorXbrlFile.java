package ch.pschatzmann.edgar.dataload;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Timer;

import org.apache.log4j.Logger;

import ch.pschatzmann.edgar.base.errors.NoNewDataException;
import ch.pschatzmann.edgar.dataload.background.IProcess;
import ch.pschatzmann.edgar.dataload.background.ProcessorTimerTask;
import ch.pschatzmann.edgar.dataload.rss.FeedInfoRecord;
import ch.pschatzmann.edgar.dataload.rss.RSSDataSource;
import ch.pschatzmann.edgar.utils.Utils;

/**
 * Main program which loads all the zipped xbrl files from EDGAR and stores them
 * in the local file system
 * 
 * @author pschatzmann
 *
 */

public class DownloadProcessorXbrlFile implements IProcess {
	private static final Logger LOG = Logger.getLogger(DownloadProcessorXbrlFile.class);
	private DateFormat df = new SimpleDateFormat("yyyyMMdd");
	private String destinationFile = null;
	private boolean history;

	public DownloadProcessorXbrlFile() {
		destinationFile = Utils.getProperty("destinationFolder", "/usr/local/bin/SmartEdgar/data/");
	}

	public static void main(String[] args) {
		try {
			DownloadProcessorXbrlFile main = new DownloadProcessorXbrlFile();
			String minutes = Utils.getProperty("timer", "");
			if (Utils.isEmpty(minutes)) {
				main.process();
			} else {
				LOG.info("Load will be repeaded every " + minutes + " minutes");
				Timer timer = new Timer(true);
				timer.scheduleAtFixedRate(new ProcessorTimerTask(main), 0, 60 * 1000 * Integer.parseInt(minutes));
				Utils.waitForever();
			}
			LOG.info("*** END ***");
			System.exit(0);
		} catch (NoNewDataException ex) {
			LOG.info("*** END ***");
		} catch (Exception ex) {
			LOG.error(ex, ex);
			System.exit(-1);
		}
	}

	/**
	 * Process all data. If the history flag is true we load all history data. The
	 * formsRegex parameter is defining the regex selection which forms should be
	 * processed
	 * 
	 * (non-Javadoc)
	 * 
	 * @see ch.pschatzmann.edgar.dataload.background.IProcess#process()
	 */
	public void process() throws Exception {
		this.history = "true".equalsIgnoreCase(Utils.getProperty("history", "true"));
		LOG.info("history = " + history);

		String formsRegex = Utils.getProperty("formsRegex", "10-Q.*|10-K.*"); // ".*";
		LOG.info("formsRegex = " + formsRegex);

		for (FeedInfoRecord info : RSSDataSource.createForChanges(history).getData(formsRegex)) {
			try {
				info.download();;
			} catch(Exception ex) {
				LOG.error(ex,ex);
			}
		}
	}

}
