package helper.pdfpreprocessing.pdf;


import org.apache.pdfbox.text.TextPosition;

import java.util.List;

/**
 * Internal utility class
 */
class Match {
	public final String str;
	public final List<TextPosition> positions;

	public Match(final String str, final List<TextPosition> positions) {
		this.str = str;
		this.positions = positions;
	}
}
