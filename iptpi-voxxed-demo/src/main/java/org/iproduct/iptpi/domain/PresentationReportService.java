package org.iproduct.iptpi.domain;

import javax.swing.JComponent;

public interface PresentationReportService<P extends UpdatableReportComponent<R>, R> extends ReportService<R> {
	 P getPresentationComponent();
}
