#!/usr/bin/env python
import re

with open("src/main/java/ru/herobrine1st/fusion/Config.kt") as f:
    contents = f.read()

envs = []

for match in re.finditer(r"(\w+)(\??)\s*(?:=|by\s+lazy\s*{)\s*System\.getenv\(\"([^\"]+)\"\)", contents):
    envs.append((match.group(1), match.group(2), match.group(3)))

with open(".env.fusion.sample", "w") as f:
    f.write("# DO NOT EDIT THIS FILE\n")
    f.write("# THIS FILE IS TRACKED BY GIT AND AUTOMATICALLY GENERATED\n")
    f.write("# COPY THIS FILE TO .env.fusion.local IF YOU WANT TO USE IT\n")
    for type, nullable, name in sorted(envs, key=lambda x: x[2]):
        f.write(f"{name}=[{type}{nullable}]\n")
