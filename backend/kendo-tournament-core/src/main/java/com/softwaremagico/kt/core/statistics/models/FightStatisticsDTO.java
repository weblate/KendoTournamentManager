package com.softwaremagico.kt.core.statistics.models;

/*-
 * #%L
 * Kendo Tournament Manager (Core)
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

public class FightStatisticsDTO {
    private Integer fightsNumber;
    private Integer fightsByTeam;
    private Integer duelsNumber;
    //In seconds.
    private Long time;

    public Integer getFightsNumber() {
        return fightsNumber;
    }

    public void setFightsNumber(Integer fightsNumber) {
        this.fightsNumber = fightsNumber;
    }

    public Integer getDuelsNumber() {
        return duelsNumber;
    }

    public void setDuelsNumber(Integer duelsNumber) {
        this.duelsNumber = duelsNumber;
    }

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }

    public Integer getFightsByTeam() {
        return fightsByTeam;
    }

    public void setFightsByTeam(Integer fightsByTeam) {
        this.fightsByTeam = fightsByTeam;
    }
}