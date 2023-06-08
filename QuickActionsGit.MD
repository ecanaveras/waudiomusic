# Acciones en GitHub

## Crear un repositorio

Para crear un nuevo repositorio en GitHub, sigue estos pasos:

1. Haz clic en el botón "New" en la página principal de GitHub.
2. Proporciona un nombre para el repositorio y una breve descripción.

## Clonar un repositorio

Puedes clonar un repositorio de GitHub utilizando el comando `git clone`. Por ejemplo:

git clone https://github.com/usuario/repositorio.git


## Crear una rama

Puedes crear una nueva rama en un repositorio utilizando el comando `git branch`. Por ejemplo:

git branch nueva-rama


## Agregar cambios

Después de realizar modificaciones en tus archivos, puedes agregar los cambios utilizando el comando `git add`. Por ejemplo:

git add archivo-modificado.js


## Confirmar cambios

Después de agregar los cambios, puedes confirmarlos utilizando el comando `git commit` y proporcionando un mensaje descriptivo. Por ejemplo:

git commit -m "Agregar nuevas funcionalidades al proyecto"


## Enviar cambios

Puedes enviar los cambios confirmados al repositorio remoto utilizando el comando `git push`. Por ejemplo:

git push origin rama-actualizada


# Eliminar y fusionar ramas en Git

## Eliminar una rama local

Para eliminar una rama local en Git, utiliza el comando `git branch -d <nombre_rama>`. Por ejemplo:

git branch -d rama-a-borrar


## Eliminar una rama remota

Para eliminar una rama remota en Git, utiliza el comando `git push origin --delete <nombre_rama>`. Por ejemplo:

git push origin --delete rama-a-borrar


## Fusionar ramas

Para fusionar una rama con otra en Git, primero asegúrate de estar en la rama objetivo (la rama en la que deseas fusionar los cambios). Luego, utiliza el comando `git merge <nombre_rama>` para fusionar la rama especificada. Por ejemplo:

git checkout rama-destino
git merge rama-a-fusionar


Ten en cuenta que es posible que se produzcan conflictos de fusión en caso de que haya cambios conflictivos en ambas ramas. En ese caso, deberás resolver los conflictos manualmente antes de completar la fusión.

