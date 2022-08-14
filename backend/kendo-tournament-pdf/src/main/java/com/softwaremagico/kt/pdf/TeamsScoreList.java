package com.softwaremagico.kt.pdf;

/*-
 * #%L
 * Kendo Tournament Manager (PDF)
 * %%
 * Copyright (C) 2021 - 2022 Softwaremagico
 * %%
 * This software is designed by Jorge Hortelano Otero. Jorge Hortelano Otero
 * <softwaremagico@gmail.com> Valencia (Spain).
 *  
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *  
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *  
 * You should have received a copy of the GNU General Public License along with
 * this program; If not, see <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import com.lowagie.text.*;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.softwaremagico.kt.core.controller.models.TournamentDTO;
import com.softwaremagico.kt.core.score.ScoreOfTeam;
import org.springframework.context.MessageSource;

import java.util.List;
import java.util.Locale;

public class TeamsScoreList extends ParentList {

    private static final int TABLE_BORDER = 0;

    private final TournamentDTO tournament;
    private final List<ScoreOfTeam> teamTopTen;

    private final MessageSource messageSource;

    private final Locale locale;

    public TeamsScoreList(MessageSource messageSource, Locale locale, TournamentDTO tournament, List<ScoreOfTeam> teamTopTen) {
        this.tournament = tournament;
        this.teamTopTen = teamTopTen;
        this.messageSource = messageSource;
        this.locale = locale;
    }

    @Override
    public void createBodyRows(Document document, PdfPTable mainTable, float width, float height, PdfWriter writer,
                               BaseFont font, int fontSize) {
        mainTable.addCell(getCell(messageSource.getMessage("classification.teams.name", null, locale),
                PdfTheme.getBasicFont(), 0, Element.ALIGN_CENTER, Font.BOLD));
        mainTable.addCell(getCell(messageSource.getMessage("classification.teams.fights.won", null, locale),
                PdfTheme.getBasicFont(), 0, Element.ALIGN_CENTER, Font.BOLD));
        mainTable.addCell(getCell(messageSource.getMessage("classification.teams.duels.won", null, locale),
                PdfTheme.getBasicFont(), 0, Element.ALIGN_CENTER, Font.BOLD));
        mainTable.addCell(getCell(messageSource.getMessage("classification.teams.hits", null, locale),
                PdfTheme.getBasicFont(), 0, Element.ALIGN_CENTER, Font.BOLD));
        mainTable.addCell(getCell(messageSource.getMessage("classification.teams.fights", null, locale),
                PdfTheme.getBasicFont(), 0, Element.ALIGN_CENTER, Font.BOLD));

        for (final ScoreOfTeam scoreOfTeam : teamTopTen) {
            mainTable.addCell(getCell(scoreOfTeam.getTeam().getName(), PdfTheme.getHandwrittenFont(), 1, Element.ALIGN_CENTER));
            mainTable.addCell(getCell(scoreOfTeam.getWonFights() + "/" + scoreOfTeam.getDrawFights(), 1,
                    Element.ALIGN_CENTER));
            mainTable.addCell(getCell(scoreOfTeam.getWonDuels() + "/" + scoreOfTeam.getDrawDuels(), 1,
                    Element.ALIGN_CENTER));
            mainTable.addCell(getCell("" + scoreOfTeam.getHits(), 1, Element.ALIGN_CENTER));
            mainTable.addCell(getCell("" + scoreOfTeam.getFightsDone(), 1, Element.ALIGN_CENTER));
        }
    }

    @Override
    public float[] getTableWidths() {
        return new float[]{0.40f, 0.20f, 0.20f, 0.20f, 0.20f};
    }

    @Override
    public void setTableProperties(PdfPTable mainTable) {
        mainTable.getDefaultCell().setHorizontalAlignment(Element.ALIGN_CENTER);
        mainTable.getDefaultCell().setBorder(TABLE_BORDER);
        mainTable.getDefaultCell().setBorderColor(BaseColor.BLACK);
        mainTable.setWidthPercentage(100);
    }

    @Override
    public void createHeaderRow(Document document, PdfPTable mainTable, float width, float height, PdfWriter writer,
                                BaseFont font, int fontSize) {
        final PdfPCell cell = new PdfPCell(new Paragraph(tournament.getName(), new Font(font, fontSize)));
        cell.setColspan(getTableWidths().length);
        cell.setBorderWidth(HEADER_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        mainTable.addCell(cell);
    }

    @Override
    public void createFooterRow(Document document, PdfPTable mainTable, float width, float height, PdfWriter writer,
                                BaseFont font, int fontSize) {
        mainTable.addCell(getEmptyRow());
    }

    @Override
    protected void addDocumentWriterEvents(PdfWriter writer) {
        // No background.
    }

    @Override
    protected Rectangle getPageSize() {
        return PageSize.A4;
    }
}