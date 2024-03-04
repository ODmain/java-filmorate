package ru.yandex.practicum.filmorate.storage.mpa;

import ru.yandex.practicum.filmorate.model.Mpa;

import java.util.List;

public interface MpaStorage {
    Mpa createMpa(Mpa mpa);

    Mpa getMpa(int id);

    List<Mpa> getAllMpa();

    Mpa updateMpa(Mpa mpa);
}
