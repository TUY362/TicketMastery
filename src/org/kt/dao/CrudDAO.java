package org.kt.dao;

import java.util.List;
import java.util.Optional;
import org.kt.exceptions.DBException;

public interface CrudDAO<T> {

    int insertar(T entidad) throws DBException;

    void actualizar(T entidad) throws DBException;

    void eliminar(int id) throws DBException;

    List<T> listar() throws DBException;

    Optional<T> buscarPorId(int id) throws DBException;
}