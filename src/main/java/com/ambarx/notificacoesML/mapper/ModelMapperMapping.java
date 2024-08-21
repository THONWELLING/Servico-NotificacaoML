package com.ambarx.notificacoesML.mapper;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ModelMapperMapping {

    private static final ModelMapper mapper = new ModelMapper();

    public static <Origin, Destination> Destination parseObject(Origin origin, Class<Destination> destination) {
    return mapper.map(origin, destination);
    }
    public static <Origin, Destination> List<Destination> parseListObjects(List<Origin> origin, Class<Destination> destination) {
    return origin.stream()
        .map(origem -> mapper.map(origem, destination))
        .collect(Collectors.toList());
    }
}