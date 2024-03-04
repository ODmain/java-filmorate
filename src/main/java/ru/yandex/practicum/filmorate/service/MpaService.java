package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.ValidException;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.storage.mpa.MpaStorage;

import java.util.List;

@RequiredArgsConstructor
@Service
public class MpaService {
    private final MpaStorage mpaStorage;

    public Mpa createMpa(Mpa mpa) {
        return mpaStorage.createMpa(mpa);
    }

    public Mpa getMpa(int id) {
        Mpa mpa = mpaStorage.getMpa(id);
        if (mpa == null) {
            throw new ValidException("MPA не найден", HttpStatus.NOT_FOUND);
        }
        return mpa;
    }

    public List<Mpa> getAllMpa() {
        return mpaStorage.getAllMpa();
    }

    public Mpa updateMpa(Mpa mpa) {
        Mpa updatedMpa = mpaStorage.updateMpa(mpa);
        if (updatedMpa == null) {
            throw new ValidException("MPA не найден", HttpStatus.NOT_FOUND);
        }
        return updatedMpa;
    }
}
