package com.softwaremagico.kt.core.controller;

/*-
 * #%L
 * Kendo Tournament Manager (Core)
 * %%
 * Copyright (C) 2021 - 2023 Softwaremagico
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

import com.softwaremagico.kt.core.controller.models.ParticipantDTO;
import com.softwaremagico.kt.core.controller.models.ParticipantImageDTO;
import com.softwaremagico.kt.core.converters.ParticipantConverter;
import com.softwaremagico.kt.core.converters.ParticipantImageConverter;
import com.softwaremagico.kt.core.converters.models.ParticipantConverterRequest;
import com.softwaremagico.kt.core.converters.models.ParticipantImageConverterRequest;
import com.softwaremagico.kt.core.exceptions.DataInputException;
import com.softwaremagico.kt.core.exceptions.ParticipantNotFoundException;
import com.softwaremagico.kt.core.images.ImageUtils;
import com.softwaremagico.kt.core.providers.ParticipantImageProvider;
import com.softwaremagico.kt.core.providers.ParticipantProvider;
import com.softwaremagico.kt.logger.KendoTournamentLogger;
import com.softwaremagico.kt.persistence.entities.Participant;
import com.softwaremagico.kt.persistence.entities.ParticipantImage;
import com.softwaremagico.kt.persistence.repositories.ParticipantImageRepository;
import com.softwaremagico.kt.persistence.values.ImageFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Controller
public class ParticipantImageController extends BasicInsertableController<ParticipantImage, ParticipantImageDTO, ParticipantImageRepository,
        ParticipantImageProvider, ParticipantImageConverterRequest, ParticipantImageConverter> {
    private final ParticipantConverter participantConverter;
    private final ParticipantProvider participantProvider;


    @Autowired
    public ParticipantImageController(ParticipantImageProvider provider, ParticipantImageConverter converter,
                                      ParticipantConverter participantConverter, ParticipantProvider participantProvider) {
        super(provider, converter);
        this.participantConverter = participantConverter;
        this.participantProvider = participantProvider;
    }

    @Override
    protected ParticipantImageConverterRequest createConverterRequest(ParticipantImage participantImage) {
        return new ParticipantImageConverterRequest(participantImage);
    }

    public int deleteByParticipantId(Integer participantId) {
        final Participant participant = participantProvider.get(participantId)
                .orElseThrow(() -> new ParticipantNotFoundException(getClass(), "No participant found with id '" + participantId + "'."));
        return getProvider().delete(participant);
    }

    public List<ParticipantImageDTO> get(List<ParticipantDTO> participantDTOS) {
        return convertAll(getProvider().get(participantConverter.reverseAll(participantDTOS)));
    }

    public ParticipantImageDTO getByParticipantId(Integer participantId) {
        final Participant participant = participantProvider.get(participantId)
                .orElseThrow(() -> new ParticipantNotFoundException(getClass(), "No participant found with id '" + participantId + "'."));
        return convert(getProvider().get(participant).orElse(null));
    }

    public ParticipantImageDTO add(MultipartFile file, Integer participantId, String username) {
        final ParticipantDTO participantDTO = participantConverter.convert(new ParticipantConverterRequest(participantProvider.get(participantId)
                .orElseThrow(() -> new ParticipantNotFoundException(getClass(), "No participant found with id '" + participantId + "'."))));
        return add(file, participantDTO, username);
    }

    public ParticipantImageDTO add(MultipartFile file, ParticipantDTO participantDTO, String username) throws DataInputException {
        try {
            delete(participantDTO);
            final ParticipantImage participantImage = new ParticipantImage();
            participantImage.setParticipant(participantConverter.reverse(participantDTO));
            participantImage.setData(ImageUtils.getBytes(ImageUtils.cropImage(
                    ImageUtils.resizeImage(ImageUtils.getImage(file.getBytes())))));
            participantImage.setImageFormat(ImageFormat.BASE64);
            participantImage.setCreatedBy(username);
            participantDTO.setHasAvatar(true);
            participantProvider.save(participantConverter.reverse(participantDTO));
            return convert(getProvider().save(participantImage));
        } catch (IOException e) {
            throw new DataInputException(this.getClass(), "File creation failed.");
        }
    }

    public ParticipantImageDTO add(ParticipantImageDTO participantImageDTO, String username) throws DataInputException {
        delete(participantImageDTO.getParticipant());
        participantImageDTO.setCreatedBy(username);
        try {
            participantImageDTO.setData(ImageUtils.getBytes(ImageUtils.cropImage(
                    ImageUtils.resizeImage(ImageUtils.getImage(participantImageDTO.getData())))));
        } catch (IOException e) {
            KendoTournamentLogger.warning(this.getClass(), "Image cannot be cropped");
        }
        final Participant participant = participantConverter.reverse(participantImageDTO.getParticipant());
        participant.setHasAvatar(participantImageDTO.getData() != null);
        participantProvider.save(participant);
        return convert(getProvider().save(reverse(participantImageDTO)));
    }

    public int delete(ParticipantDTO participantDTO) {
        final Participant participant = participantConverter.reverse(participantDTO);
        participant.setHasAvatar(false);
        participantProvider.save(participant);
        return getProvider().delete(participant);
    }
}
