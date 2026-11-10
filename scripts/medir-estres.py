#!/usr/bin/env python3
"""Repite cargas en JVM independientes y resume sus mediciones. Solo usa la biblioteca estándar."""

import argparse
import csv
import datetime
import json
import os
from pathlib import Path
import statistics
import subprocess
import sys


def entero_entre(minimo, maximo):
    def convertir(texto):
        try:
            valor = int(texto)
        except ValueError as error:
            raise argparse.ArgumentTypeError("Se requiere un entero") from error
        if not minimo <= valor <= maximo:
            raise argparse.ArgumentTypeError(f"Usa un valor entre {minimo} y {maximo}")
        return valor
    return convertir


def medir():
    raiz = Path(__file__).resolve().parents[1]
    java_home = os.environ.get("JAVA_HOME")
    java = str(Path(java_home) / "bin" / "java") if java_home else "java"
    fecha = datetime.datetime.now(datetime.timezone.utc).strftime("%Y%m%dT%H%M%SZ")
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--java", default=java)
    parser.add_argument("--jar", type=Path, default=raiz / "trama8583-escenarios/target/trama8583-escenarios-1.0-SNAPSHOT.jar")
    parser.add_argument("--hilos", default="1,4", help="Lista separada por comas; cada valor entre 1 y 64")
    parser.add_argument("--cargas", help="Lista separada por comas; por defecto consulta todas las cargas del JAR")
    parser.add_argument("--segundos", type=entero_entre(1, 1800), default=10)
    parser.add_argument("--calentamiento", type=entero_entre(0, 60), default=2)
    parser.add_argument("--repeticiones", type=entero_entre(1, 10), default=3)
    parser.add_argument("--heap-mib", type=entero_entre(64, 65536), default=256)
    parser.add_argument("--salida", type=Path, default=raiz / "target/estres" / fecha)
    opciones = parser.parse_args()
    try:
        hilos = list(dict.fromkeys(entero_entre(1, 64)(valor) for valor in opciones.hilos.split(",")))
    except argparse.ArgumentTypeError as error:
        parser.error(str(error))
    if not opciones.jar.is_file():
        parser.error("No existe el JAR; ejecuta primero Maven verify o indica --jar")

    comando = [opciones.java, f"-Xms{opciones.heap_mib}m", f"-Xmx{opciones.heap_mib}m", "-jar", str(opciones.jar.resolve()), "--estres"]
    listado = subprocess.run(comando + ["--listar"], capture_output=True, text=True, timeout=30, check=True)
    disponibles = listado.stdout.splitlines()
    cargas = list(dict.fromkeys(opciones.cargas.split(","))) if opciones.cargas else disponibles
    if not cargas or any(carga not in disponibles for carga in cargas):
        parser.error("Cargas válidas: " + ", ".join(disponibles))
    destino = opciones.salida.resolve()
    destino.mkdir(parents=True, exist_ok=True)
    resumen = []
    for carga in cargas:
        for concurrencia in hilos:
            mediciones = []
            for repeticion in range(1, opciones.repeticiones + 1):
                reporte = destino / f"{carga}-{concurrencia}h-{repeticion}.json"
                resultado = subprocess.run(comando + ["--carga", carga, "--hilos", str(concurrencia),
                    "--segundos", str(opciones.segundos), "--calentamiento", str(opciones.calentamiento),
                    "--salida", str(reporte)], capture_output=True, text=True,
                    timeout=opciones.segundos + opciones.calentamiento + 30)
                if resultado.returncode != 0:
                    print(resultado.stdout, file=sys.stderr)
                    print(resultado.stderr, file=sys.stderr)
                    raise RuntimeError(f"Falló {carga}, {concurrencia} hilos, repetición {repeticion}")
                datos = json.loads(reporte.read_text(encoding="utf-8"))
                if not datos["cumple"] or datos["erroresInesperados"] or datos["operaciones"] <= 0:
                    raise RuntimeError("El reporte contiene errores o no cumple las comprobaciones")
                mediciones.append(datos)
            fila = dict(carga=carga, hilos=concurrencia, repeticiones=opciones.repeticiones,
                medianaOpsPorSegundo=statistics.median(item["operacionesPorSegundo"] for item in mediciones),
                medianaP95Ms=statistics.median(item["p95NsLimiteSuperior"] / 1e6 for item in mediciones),
                medianaP99Ms=statistics.median(item["p99NsLimiteSuperior"] / 1e6 for item in mediciones),
                operacionesTotales=sum(item["operaciones"] for item in mediciones), erroresInesperados=0)
            resumen.append(fila)
            # Conserva también resultados parciales si otra combinación falla después.
            (destino / "resumen.json").write_text(json.dumps(resumen, indent=2) + "\n", encoding="utf-8")
            with (destino / "resumen.csv").open("w", newline="", encoding="utf-8") as archivo:
                escritor = csv.DictWriter(archivo, fieldnames=resumen[0].keys())
                escritor.writeheader()
                escritor.writerows(resumen)
            print(json.dumps(fila), flush=True)

    try:
        revision = subprocess.run(["git", "rev-parse", "HEAD"], cwd=raiz, capture_output=True, text=True, timeout=10, check=True).stdout.strip()
        cambios = bool(subprocess.run(["git", "status", "--porcelain"], cwd=raiz, capture_output=True, text=True, timeout=10, check=True).stdout)
    except (OSError, subprocess.SubprocessError):
        revision, cambios = "desconocida", None
    (destino / "ejecucion.json").write_text(json.dumps(dict(revisionGit=revision, cambiosSinCommit=cambios,
        segundos=opciones.segundos, calentamiento=opciones.calentamiento, repeticiones=opciones.repeticiones,
        heapMiB=opciones.heap_mib, cargas=cargas, hilos=hilos), indent=2) + "\n", encoding="utf-8")
    print("Resumen: " + str(destino / "resumen.csv"))


if __name__ == "__main__":
    try:
        medir()
    except (OSError, subprocess.SubprocessError, RuntimeError, ValueError, KeyError) as error:
        print("No se pudo completar la matriz: " + str(error), file=sys.stderr)
        sys.exit(1)
