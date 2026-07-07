#!/usr/bin/env bash

export PATH="$HOME/.local/bin:$PATH"

CONFIG_FILE="${XDG_CONFIG_HOME:-$HOME/.config}/mutation-ai/config.env"
if [ -f "$CONFIG_FILE" ]; then
  # shellcheck disable=SC1090
  . "$CONFIG_FILE"
fi
