#!/bin/sh
# Entry point of the emulator image: picks the renderer, then runs the emulator.
#
# With a GPU render node passed in (tools/run-emulator.sh adds /dev/dri when
# the host has one), the emulator renders on the GPU through -gpu host.
# That mode needs an X display even with -no-window: without one the GLES
# translator fails with "Failed to open display" and never starts, and the
# EGL-only route (ANDROID_EGL_ON_EGL) falls back to SwiftShader on its own.
# A headless weston with Xwayland supplies that display without a monitor.
#
# Without a usable render node, or when weston does not come up, the
# emulator renders on the CPU with swiftshader_indirect, as before.
#
# EMULATOR_GPU=host|swiftshader_indirect overrides the choice.
set -eu

render_node_usable() {
    for node in /dev/dri/renderD*; do
        [ -e "${node}" ] || continue
        # Opening is the only reliable test: with rootless podman the node
        # can be present but owned by a group the container does not have.
        if (exec 3<> "${node}") 2> /dev/null; then
            return 0
        fi
    done
    return 1
}

start_display() {
    export XDG_RUNTIME_DIR=/tmp/xdg
    mkdir -p "${XDG_RUNTIME_DIR}" /tmp/.X11-unix
    chmod 700 "${XDG_RUNTIME_DIR}"
    # Xwayland creates its socket here and gives up silently when it cannot.
    chmod 1777 /tmp/.X11-unix

    weston --backend=headless --renderer=gl --xwayland --socket=wl0 \
        --idle-time=0 > /tmp/weston.log 2>&1 &

    i=0
    while [ "${i}" -lt 30 ]; do
        for sock in /tmp/.X11-unix/X*; do
            if [ -S "${sock}" ]; then
                export DISPLAY=":${sock#/tmp/.X11-unix/X}"
                return 0
            fi
        done
        sleep 1
        i=$((i + 1))
    done
    return 1
}

gpu="${EMULATOR_GPU:-}"
if [ -z "${gpu}" ]; then
    if render_node_usable; then
        gpu=host
    else
        gpu=swiftshader_indirect
    fi
fi

if [ "${gpu}" = host ] && ! start_display; then
    echo "emulator-entrypoint: weston did not start, falling back to swiftshader_indirect" >&2
    tail -n 20 /tmp/weston.log >&2 || true
    gpu=swiftshader_indirect
fi

echo "emulator-entrypoint: -gpu ${gpu}"
exec emulator -avd phone -no-window -no-audio -no-boot-anim -no-snapshot \
    -gpu "${gpu}" "$@"
