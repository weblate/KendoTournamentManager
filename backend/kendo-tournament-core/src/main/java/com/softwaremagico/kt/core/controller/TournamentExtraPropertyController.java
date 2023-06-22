package com.softwaremagico.kt.core.controller;

/*-
 * #%L
 * Kendo Tournament Manager (Core)
 * %%
 * Copyright (C) 2021 - 2023 Softwaremagico
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

import com.softwaremagico.kt.core.controller.models.TournamentExtraPropertyDTO;
import com.softwaremagico.kt.core.converters.TournamentConverter;
import com.softwaremagico.kt.core.converters.TournamentExtraPropertyConverter;
import com.softwaremagico.kt.core.converters.models.TournamentExtraPropertyConverterRequest;
import com.softwaremagico.kt.core.exceptions.TournamentNotFoundException;
import com.softwaremagico.kt.core.providers.TournamentExtraPropertyProvider;
import com.softwaremagico.kt.core.providers.TournamentProvider;
import com.softwaremagico.kt.persistence.entities.TournamentExtraProperty;
import com.softwaremagico.kt.persistence.repositories.TournamentExtraPropertyRepository;
import com.softwaremagico.kt.persistence.values.TournamentExtraPropertyKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class TournamentExtraPropertyController extends BasicInsertableController<TournamentExtraProperty, TournamentExtraPropertyDTO,
        TournamentExtraPropertyRepository, TournamentExtraPropertyProvider, TournamentExtraPropertyConverterRequest, TournamentExtraPropertyConverter> {

    private final TournamentProvider tournamentProvider;
    private final TournamentConverter tournamentConverter;


    @Autowired
    protected TournamentExtraPropertyController(TournamentExtraPropertyProvider provider, TournamentExtraPropertyConverter converter,
                                                TournamentProvider tournamentProvider, TournamentConverter tournamentConverter) {
        super(provider, converter);
        this.tournamentProvider = tournamentProvider;
        this.tournamentConverter = tournamentConverter;
    }

    @Override
    protected TournamentExtraPropertyConverterRequest createConverterRequest(TournamentExtraProperty tournamentExtraProperty) {
        return new TournamentExtraPropertyConverterRequest(tournamentExtraProperty);
    }

    @Override
    public TournamentExtraPropertyDTO update(TournamentExtraPropertyDTO dto, String username) {
        getProvider().deleteByTournamentAndProperty(tournamentConverter.reverse(dto.getTournament()), dto.getProperty());
        dto.setUpdatedBy(username);
        return create(dto, null);
    }

    public List<TournamentExtraPropertyDTO> getByTournamentId(Integer tournamentId) {
        return convertAll(getProvider().getAll(tournamentProvider.get(tournamentId)
                .orElseThrow(() -> new TournamentNotFoundException(getClass(), "No tournament found with id '" + tournamentId + "'."))));
    }

    public TournamentExtraPropertyDTO getByTournamentAndProperty(Integer tournamentId, TournamentExtraPropertyKey key) {
        return convert(getProvider().getByTournamentAndProperty(tournamentProvider.get(tournamentId)
                .orElseThrow(() -> new TournamentNotFoundException(getClass(), "No tournament found with id '" + tournamentId + "'.")), key));
    }


}
