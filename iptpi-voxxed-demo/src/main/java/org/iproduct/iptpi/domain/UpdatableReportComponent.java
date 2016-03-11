package org.iproduct.iptpi.domain;

import javax.swing.JComponent;

public abstract class UpdatableReportComponent<R> extends JComponent{
	private static final long serialVersionUID = 1L;

	public abstract void updateReport(R report);
}
