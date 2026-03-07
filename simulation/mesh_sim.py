#!/usr/bin/env python3
"""
Meshtastic 50-Node Mesh Network Simulator
Interactive + Priority Messaging + Congestion Simulation

Controls:
  CLICK 1st node    - Select sender (green)
  CLICK 2nd node    - Select destination (red) and send
  1 / 2 / 3 / 4    - Message priority: LOW / NORMAL / HIGH / EMERGENCY
  C                 - Cycle congestion level: NONE -> LOW -> MEDIUM -> HIGH
  SPACE             - Pause / Resume
  R                 - Skip to next message
  H                 - Toggle auto-message mode on/off
  HOVER             - Inspect node details
  Q / Escape        - Quit
"""

import random
import networkx as nx
import matplotlib.pyplot as plt
import matplotlib.animation as animation
import matplotlib.patches as mpatches
import matplotlib.widgets as widgets
import numpy as np
from collections import deque

# ── Config ─────────────────────────────────────────────────────────
NUM_NODES    = 50
RADIO_RANGE  = 0.22
NUM_MESSAGES = 30
SEED         = 42

random.seed(SEED)
np.random.seed(SEED)

# ── Priority definitions ────────────────────────────────────────────
PRIORITIES = {
    1: dict(name="LOW",       color="#3a86ff", path_color="#3a86ff", interval=900),
    2: dict(name="NORMAL",    color="#f72585", path_color="#f72585", interval=500),
    3: dict(name="HIGH",      color="#f4a261", path_color="#f4a261", interval=280),
    4: dict(name="EMERGENCY", color="#e63946", path_color="#ff0000", interval=120),
}

# ── Congestion levels ───────────────────────────────────────────────
CONGESTION_LEVELS = {
    0: dict(name="NONE",   color="#2ecc71", hop_extra_wait=0, hot_threshold=999),
    1: dict(name="LOW",    color="#f1c40f", hop_extra_wait=1, hot_threshold=6),
    2: dict(name="MEDIUM", color="#f4a261", hop_extra_wait=2, hot_threshold=4),
    3: dict(name="HIGH",   color="#e63946", hop_extra_wait=4, hot_threshold=2),
}

# Per-hop drop probability [congestion_level][priority]
# EMERGENCY (4) is always 0. LOW priority suffers the most.
DROP_TABLE = {
    0: {1: 0.00, 2: 0.00, 3: 0.00, 4: 0.00},
    1: {1: 0.18, 2: 0.06, 3: 0.02, 4: 0.00},
    2: {1: 0.40, 2: 0.18, 3: 0.05, 4: 0.00},
    3: {1: 0.70, 2: 0.40, 3: 0.12, 4: 0.00},
}

current_priority   = 2
current_congestion = 0

# ── Colors ──────────────────────────────────────────────────────────
BG          = "#0a0e1a"
EDGE_IDLE   = "#1a2a4a"
NODE_ROUTER = "#00b4d8"
NODE_CLIENT = "#48cae4"
NODE_VISIT  = "#7209b7"
NODE_ACTIVE = "#ffffff"
NODE_HOVER  = "#f4a261"
NODE_SRC    = "#2ecc71"
NODE_DST    = "#e74c3c"
NODE_SELECT = "#f1c40f"
NODE_HOT1   = "#f9c74f"   # warm  (low load)
NODE_HOT2   = "#f8961e"   # hot   (medium load)
NODE_HOT3   = "#f94144"   # overloaded
TEXT        = "#e0e0e0"
DIM         = "#555555"

# ── Build network ───────────────────────────────────────────────────
def rand_node_id():
    return f"!{random.randint(0x10000000, 0xFFFFFFFF):08x}"

clusters = [(random.uniform(0.1, 0.9), random.uniform(0.1, 0.9)) for _ in range(8)]
positions = {}
for i in range(NUM_NODES):
    cx, cy = random.choice(clusters)
    x = float(np.clip(random.gauss(cx, 0.14), 0.03, 0.97))
    y = float(np.clip(random.gauss(cy, 0.14), 0.03, 0.97))
    positions[i] = (x, y)

node_ids     = [rand_node_id() for _ in range(NUM_NODES)]
node_roles   = [random.choice(["ROUTER","ROUTER","ROUTER_CLIENT","CLIENT","CLIENT","CLIENT"])
                for _ in range(NUM_NODES)]
node_battery = [random.randint(15, 100) for _ in range(NUM_NODES)]
node_snr     = [round(random.uniform(-12, 12), 1) for _ in range(NUM_NODES)]
node_load    = [0] * NUM_NODES   # how many messages routed through each node

G = nx.Graph()
G.add_nodes_from(range(NUM_NODES))
for i in range(NUM_NODES):
    for j in range(i+1, NUM_NODES):
        xi, yi = positions[i]; xj, yj = positions[j]
        d = ((xi-xj)**2 + (yi-yj)**2)**0.5
        if d <= RADIO_RANGE:
            G.add_edge(i, j, weight=d)

components = list(nx.connected_components(G))
while len(components) > 1:
    c1, c2 = list(components[0]), list(components[1])
    best = min(((n1, n2, ((positions[n1][0]-positions[n2][0])**2+(positions[n1][1]-positions[n2][1])**2)**0.5)
                for n1 in c1 for n2 in c2), key=lambda t: t[2])
    G.add_edge(best[0], best[1], weight=best[2])
    components = list(nx.connected_components(G))

# ── Priority queue ──────────────────────────────────────────────────
prio_queue = deque()

def enqueue_auto_messages(n=NUM_MESSAGES):
    for _ in range(n):
        p = random.choice([1, 1, 2, 2, 2, 3, 4])
        src = random.randint(0, NUM_NODES-1)
        dst = random.randint(0, NUM_NODES-1)
        while dst == src:
            dst = random.randint(0, NUM_NODES-1)
        prio_queue.append((p, src, dst))

enqueue_auto_messages()

# ── Simulation state ────────────────────────────────────────────────
state = dict(
    path=[], step=0, src=None, dst=None, priority=2,
    wait=0, paused=False, auto_mode=True,
    total=0, delivered=0, dropped=0, hops_sum=0,
    log=[], hovered=None,
    select_src=None,
    frame_interval=500,
    hop_extra=0,
    dropped_mid=False,
)

def drop_message(src, dst, priority):
    """Record a dropped message."""
    state["total"]   += 1
    state["dropped"] += 1
    cname = CONGESTION_LEVELS[current_congestion]["name"]
    pname = PRIORITIES[priority]["name"]
    state["log"].insert(0,
        f"  [DROP/{cname}] {node_ids[src][-6:]} -> {node_ids[dst][-6:]}  ({pname})")
    state["log"] = state["log"][:16]

def load_message(src, dst, priority):
    cong = CONGESTION_LEVELS[current_congestion]
    state["src"]           = src
    state["dst"]           = dst
    state["priority"]      = priority
    state["total"]        += 1
    state["step"]          = 0
    state["frame_interval"] = PRIORITIES[priority]["interval"]
    state["hop_extra"]     = cong["hop_extra_wait"]
    state["dropped_mid"]   = False

    try:
        path = nx.shortest_path(G, src, dst, weight="weight")
        state["path"] = path
        hops = len(path) - 1
        state["hops_sum"] += hops
        state["delivered"] += 1
        # Increment load for every intermediate node
        for n in path[1:-1]:
            node_load[n] += 1
        pname = PRIORITIES[priority]["name"]
        cname = CONGESTION_LEVELS[current_congestion]["name"]
        suffix = f"  [{cname}]" if current_congestion > 0 else ""
        state["log"].insert(0,
            f"  [{pname[:2]}] {node_ids[src][-6:]} -> {node_ids[dst][-6:]}  {hops} hop{'s' if hops!=1 else ''}{suffix}")
    except nx.NetworkXNoPath:
        state["path"] = [src]
        state["log"].insert(0,
            f"  [--] {node_ids[src][-6:]} -> {node_ids[dst][-6:]}  no path")
    state["log"] = state["log"][:16]

def load_next_auto():
    global prio_queue
    if not prio_queue:
        enqueue_auto_messages()
    best_idx, best_p = 0, -1
    for i, (p, _, _) in enumerate(prio_queue):
        if p > best_p:
            best_p, best_idx = p, i
    items = list(prio_queue)
    p, src, dst = items.pop(best_idx)
    prio_queue = deque(items)
    load_message(src, dst, p)

load_next_auto()

# ── Figure ──────────────────────────────────────────────────────────
fig = plt.figure(figsize=(17, 9), facecolor=BG)

ax_map    = fig.add_axes([0.01, 0.10, 0.64, 0.84])
ax_info   = fig.add_axes([0.67, 0.52, 0.32, 0.42])
ax_log    = fig.add_axes([0.67, 0.10, 0.32, 0.38])
ax_slider = fig.add_axes([0.07, 0.03, 0.52, 0.03])

for ax in (ax_map, ax_info, ax_log):
    ax.set_facecolor(BG)
    for spine in ax.spines.values():
        spine.set_edgecolor("#1e3a5f")

ax_map.set_xlim(-0.03, 1.03)
ax_map.set_ylim(-0.03, 1.03)
ax_map.set_xticks([]); ax_map.set_yticks([])
ax_info.axis("off"); ax_log.axis("off")

fig.text(0.01, 0.988, "Meshtastic  |  50-Node Mesh Simulation",
         color="white", fontsize=12, fontweight="bold", va="top")

prio_hud = fig.text(0.38, 0.988, "", color="white", fontsize=9,
                    va="top", ha="left", family="monospace")
cong_hud = fig.text(0.70, 0.988, "", color="white", fontsize=9,
                    va="top", ha="left", family="monospace")

fig.text(0.99, 0.988,
         "CLICK src->dst  |  1-4=priority  |  SPACE=pause  |  H=auto  |  Q=quit",
         color=DIM, fontsize=7.5, va="top", ha="right")

ax_map.set_title("Network Topology", color=TEXT, fontsize=9, pad=3)
ax_info.set_title("Node / Transmission Info", color=TEXT, fontsize=10, pad=3)
ax_log.set_title("Message Log", color=TEXT, fontsize=10, pad=3)

# ── Node color / size helpers ───────────────────────────────────────
def heat_color(node):
    """Return a heat color based on node_load and congestion threshold."""
    thresh = CONGESTION_LEVELS[current_congestion]["hot_threshold"]
    load = node_load[node]
    if load >= thresh * 3:   return NODE_HOT3
    elif load >= thresh * 2: return NODE_HOT2
    elif load >= thresh:     return NODE_HOT1
    return None   # not hot

def base_color(node):
    h = heat_color(node)
    if h:
        return h
    return NODE_ROUTER if node_roles[node] in ("ROUTER","ROUTER_CLIENT") else NODE_CLIENT

def node_colors(visited=(), active=None, hovered=None, src=None, dst=None,
                select_src=None, prio=2):
    c = []
    for i in range(NUM_NODES):
        if i == active:        c.append(NODE_ACTIVE)
        elif i == select_src:  c.append(NODE_SELECT)
        elif i == hovered:     c.append(NODE_HOVER)
        elif i == src:         c.append(NODE_SRC)
        elif i == dst:         c.append(NODE_DST)
        elif i in visited:     c.append(NODE_VISIT)
        else:                  c.append(base_color(i))
    return c

def node_sizes(visited=(), active=None, hovered=None, select_src=None):
    s = []
    for i in range(NUM_NODES):
        if i == active:        s.append(260)
        elif i == select_src:  s.append(220)
        elif i == hovered:     s.append(200)
        elif i in visited:     s.append(170)
        elif node_roles[i] in ("ROUTER","ROUTER_CLIENT"): s.append(130)
        else:                  s.append(85)
    return s

# ── Draw static edges ───────────────────────────────────────────────
pos_arr = np.array([positions[i] for i in range(NUM_NODES)])
edge_artists = {}
for u, v in G.edges():
    xu, yu = positions[u]; xv, yv = positions[v]
    (line,) = ax_map.plot([xu,xv],[yu,yv], color=EDGE_IDLE, linewidth=0.6, zorder=1)
    edge_artists[(min(u,v),max(u,v))] = line

scat = ax_map.scatter(
    pos_arr[:,0], pos_arr[:,1],
    c=node_colors(), s=node_sizes(),
    zorder=3, linewidths=0.6, edgecolors="white", alpha=0.92
)

for i in range(NUM_NODES):
    x, y = positions[i]
    ax_map.text(x, y-0.027, node_ids[i][-5:], color=DIM,
                fontsize=3.8, ha="center", va="top", zorder=4)

(path_line,) = ax_map.plot([], [], linewidth=2.5, zorder=2, linestyle="--", alpha=0.9)
(glow,)      = ax_map.plot([], [], "o", markersize=24, zorder=5, alpha=0.3)

pause_txt = ax_map.text(0.5, 0.5, "", transform=ax_map.transAxes,
    color="white", fontsize=28, fontweight="bold", ha="center", va="center", zorder=10,
    bbox=dict(boxstyle="round,pad=0.4", facecolor="#00000099", edgecolor="white"))

click_hint = ax_map.text(0.5, 0.97, "", transform=ax_map.transAxes,
    color=NODE_SELECT, fontsize=10, fontweight="bold", ha="center", va="top", zorder=10)

stats_txt = ax_map.text(0.01, 0.995, "", transform=ax_map.transAxes,
    color=TEXT, fontsize=8, va="top", zorder=6)

legend_patches = [
    mpatches.Patch(color=NODE_ROUTER, label="Router"),
    mpatches.Patch(color=NODE_CLIENT, label="Client"),
    mpatches.Patch(color=NODE_HOT1,   label="Warm node (light traffic)"),
    mpatches.Patch(color=NODE_HOT2,   label="Hot node (heavy traffic)"),
    mpatches.Patch(color=NODE_HOT3,   label="Overloaded node"),
    mpatches.Patch(color=NODE_SELECT, label="Select: sender"),
    mpatches.Patch(color=NODE_SRC,    label="Sender"),
    mpatches.Patch(color=NODE_DST,    label="Destination"),
    mpatches.Patch(color=NODE_VISIT,  label="Path taken"),
    mpatches.Patch(color=NODE_ACTIVE, label="Current hop"),
]
ax_map.legend(handles=legend_patches, loc="lower right",
              facecolor="#0d1b2a", edgecolor="#1e3a5f",
              labelcolor="white", fontsize=7)

info_txt = ax_info.text(0.06, 0.96, "", transform=ax_info.transAxes,
    color=TEXT, fontsize=9, va="top", family="monospace")
log_txt  = ax_log.text(0.04, 0.96, "", transform=ax_log.transAxes,
    color=TEXT, fontsize=7.5, va="top", family="monospace")

# ── HUD updaters ────────────────────────────────────────────────────
def update_prio_hud():
    parts = [f"[{p['name']}]" if k == current_priority else f" {p['name']} "
             for k, p in PRIORITIES.items()]
    prio_hud.set_text("Prio: " + "  ".join(parts))
    prio_hud.set_color(PRIORITIES[current_priority]["color"])

def update_cong_hud():
    parts = [f"[{c['name']}]" if k == current_congestion else f" {c['name']} "
             for k, c in CONGESTION_LEVELS.items()]
    cong_hud.set_text("Cong: " + " ".join(parts))
    cong_hud.set_color(CONGESTION_LEVELS[current_congestion]["color"])

update_prio_hud()
update_cong_hud()

# ── Helpers ─────────────────────────────────────────────────────────
def nearest_node(x, y, threshold=0.045):
    best_i, best_d = None, float("inf")
    for i in range(NUM_NODES):
        xi, yi = positions[i]
        d = ((x-xi)**2 + (y-yi)**2)**0.5
        if d < best_d:
            best_d, best_i = d, i
    return best_i if best_d <= threshold else None

def reset_edges(prio=2):
    for line in edge_artists.values():
        line.set_color(EDGE_IDLE)
        line.set_linewidth(0.6)
    path_line.set_data([], [])
    path_line.set_color(PRIORITIES[prio]["path_color"])
    glow.set_data([], [])
    glow.set_color(PRIORITIES[prio]["color"])

# ── Event handlers ──────────────────────────────────────────────────
def on_key(event):
    global current_priority, current_congestion
    if event.key == " ":
        state["paused"] = not state["paused"]
        pause_txt.set_text("  PAUSED  " if state["paused"] else "")

    elif event.key in ("1","2","3","4"):
        current_priority = int(event.key)
        update_prio_hud()

    elif event.key in ("r","R"):
        state["select_src"] = None
        click_hint.set_text("")
        reset_edges(state["priority"])
        if state["auto_mode"]:
            load_next_auto()
        state["paused"] = False
        pause_txt.set_text("")

    elif event.key in ("h","H"):
        state["auto_mode"] = not state["auto_mode"]
        pause_txt.set_text(f"  {'AUTO ON' if state['auto_mode'] else 'MANUAL'}  ")

    elif event.key in ("q","Q","escape"):
        plt.close("all")
        return

    fig.canvas.draw_idle()

def on_click(event):
    if event.inaxes != ax_map:
        return
    node = nearest_node(event.xdata, event.ydata)
    if node is None:
        return

    if state["select_src"] is None:
        state["select_src"] = node
        click_hint.set_text(f"Sender: {node_ids[node][-8:]}   Now click destination...")
        scat.set_facecolors(node_colors(select_src=node, hovered=state["hovered"],
                                         prio=current_priority))
        scat.set_sizes(node_sizes(select_src=node, hovered=state["hovered"]))
    else:
        src = state["select_src"]
        dst = node
        state["select_src"] = None
        click_hint.set_text("")
        if src == dst:
            return
        reset_edges(current_priority)
        state["paused"] = False
        pause_txt.set_text("")
        load_message(src, dst, current_priority)

    fig.canvas.draw_idle()

def on_hover(event):
    if event.inaxes != ax_map:
        if state["hovered"] is not None:
            state["hovered"] = None
            fig.canvas.draw_idle()
        return

    node = nearest_node(event.xdata, event.ydata)
    if node == state["hovered"]:
        return
    state["hovered"] = node

    visited = set(state["path"][:state["step"]+1])
    active  = state["path"][state["step"]] if state["step"] < len(state["path"]) else None
    scat.set_facecolors(node_colors(visited=visited, active=active, hovered=node,
                                     src=state["src"], dst=state["dst"],
                                     select_src=state["select_src"],
                                     prio=state["priority"]))
    scat.set_sizes(node_sizes(visited=visited, active=active, hovered=node,
                               select_src=state["select_src"]))

    if node is not None:
        cong = CONGESTION_LEVELS[current_congestion]
        h = heat_color(node)
        heat_str = "OVERLOADED" if h == NODE_HOT3 else ("HOT" if h == NODE_HOT2 else ("WARM" if h == NODE_HOT1 else "OK"))
        info_txt.set_text(
            f"-- HOVERED NODE --\n"
            f"ID    : {node_ids[node]}\n"
            f"ROLE  : {node_roles[node]}\n"
            f"BAT   : {node_battery[node]}%\n"
            f"SNR   : {node_snr[node]} dB\n"
            f"LINKS : {G.degree(node)}\n"
            f"LOAD  : {node_load[node]} msgs  [{heat_str}]\n"
            f"\n"
            f"-- ACTIVE TX --\n"
            f"PRIO  : {PRIORITIES[state['priority']]['name']}\n"
            f"CONG  : {cong['name']} (drop {int(DROP_TABLE[current_congestion][state['priority']]*100)}%)\n"
            f"FROM  : {node_ids[state['src']] if state['src'] is not None else '-'}\n"
            f"TO    : {node_ids[state['dst']] if state['dst'] is not None else '-'}\n"
            f"HOP   : {state['step']} / {max(len(state['path'])-1,0)}\n"
            f"\n"
            f"TOTAL : {state['total']}\n"
            f"DLVRD : {state['delivered']}\n"
            f"DROP  : {state['dropped']}\n"
        )
    fig.canvas.draw_idle()

# ── Congestion slider ───────────────────────────────────────────────
ax_slider.set_facecolor("#0d1b2a")
for spine in ax_slider.spines.values():
    spine.set_edgecolor("#1e3a5f")

cong_slider = widgets.Slider(
    ax=ax_slider,
    label="Congestion",
    valmin=0, valmax=3, valinit=0, valstep=1,
    color=CONGESTION_LEVELS[0]["color"],
    track_color="#1a2a4a",
)
cong_slider.label.set_color(TEXT)
cong_slider.valtext.set_visible(False)   # hide raw number; we show name in HUD

# Tick labels: one per level
for k, cdata in CONGESTION_LEVELS.items():
    ax_slider.text(k / 3, -1.2, cdata["name"], transform=ax_slider.transAxes,
                   ha="center", va="top", color=cdata["color"], fontsize=8, fontweight="bold")

def on_slider(val):
    global current_congestion
    current_congestion = int(round(val))
    cong_slider.poly.set_facecolor(CONGESTION_LEVELS[current_congestion]["color"])
    update_cong_hud()
    visited = set(state["path"][:state["step"]+1])
    active  = state["path"][state["step"]] if state["step"] < len(state["path"]) else None
    scat.set_facecolors(node_colors(visited=visited, active=active,
                                     src=state["src"], dst=state["dst"],
                                     select_src=state["select_src"],
                                     prio=state["priority"]))
    fig.canvas.draw_idle()

cong_slider.on_changed(on_slider)

fig.canvas.mpl_connect("key_press_event", on_key)
fig.canvas.mpl_connect("button_press_event", on_click)
fig.canvas.mpl_connect("motion_notify_event", on_hover)

# ── Animation ───────────────────────────────────────────────────────
def animate(_frame):
    if state["paused"]:
        return
    if state["wait"] > 0:
        state["wait"] -= 1
        return

    path = state["path"]
    step = state["step"]
    prio = state["priority"]
    pcolor = PRIORITIES[prio]["path_color"]

    ani.event_source.interval = state["frame_interval"]

    if step >= len(path):
        reset_edges(prio)
        scat.set_facecolors(node_colors(prio=prio))
        scat.set_sizes(node_sizes())
        state["wait"] = max(1, 300 // PRIORITIES[prio]["interval"])
        pause_txt.set_text("")
        pause_txt.set_color("white")
        if state["auto_mode"]:
            load_next_auto()
        else:
            state["paused"] = True
            pause_txt.set_text("  DONE — click nodes to send  ")
        return

    state["step"] += 1
    current = path[step]
    visited = set(path[:step+1])

    # Per-hop drop check (skip src node, skip EMERGENCY)
    if step > 0 and not state["dropped_mid"]:
        drop_prob = DROP_TABLE[current_congestion][prio]
        if drop_prob > 0 and random.random() < drop_prob:
            state["dropped_mid"] = True
            state["dropped"] += 1
            cname = CONGESTION_LEVELS[current_congestion]["name"]
            pname = PRIORITIES[prio]["name"]
            src, dst = state["src"], state["dst"]
            state["log"].insert(0,
                f"  [DROP@hop{step}/{cname}] {node_ids[src][-6:]} -> {node_ids[dst][-6:]}  ({pname})")
            state["log"] = state["log"][:16]
            # Show drop flash then move to next message
            pause_txt.set_text(f"  DROPPED  ({pname} @ {cname})  ")
            pause_txt.set_color(CONGESTION_LEVELS[current_congestion]["color"])
            state["wait"] = 6
            state["step"] = len(path)   # skip to end
            return

    # Add congestion delay at hot nodes (not for EMERGENCY)
    if prio < 4 and current_congestion > 0 and heat_color(current) is not None:
        state["wait"] = state["hop_extra"]

    scat.set_facecolors(node_colors(visited=visited, active=current,
                                     hovered=state["hovered"],
                                     src=state["src"], dst=state["dst"],
                                     select_src=state["select_src"], prio=prio))
    scat.set_sizes(node_sizes(visited=visited, active=current,
                               hovered=state["hovered"],
                               select_src=state["select_src"]))

    if step > 0:
        px = [positions[n][0] for n in path[:step+1]]
        py = [positions[n][1] for n in path[:step+1]]
        path_line.set_data(px, py)
        path_line.set_color(pcolor)
        for k in range(step):
            key = (min(path[k],path[k+1]), max(path[k],path[k+1]))
            if key in edge_artists:
                edge_artists[key].set_color(pcolor)
                edge_artists[key].set_linewidth(2.5)

    cx, cy = positions[current]
    glow.set_data([cx], [cy])
    glow.set_color(PRIORITIES[prio]["color"])

    src, dst = state["src"], state["dst"]
    total_hops = len(path) - 1
    avg  = state["hops_sum"] / max(state["delivered"], 1)
    pname = PRIORITIES[prio]["name"]
    cong  = CONGESTION_LEVELS[current_congestion]

    info_txt.set_text(
        f"-- ACTIVE TX [{pname}] --\n"
        f"FROM  : {node_ids[src]}\n"
        f"TO    : {node_ids[dst]}\n"
        f"ROLE  : {node_roles[src]}\n"
        f"\n"
        f"HOP   : {step} / {total_hops}\n"
        f"AT    : {node_ids[current]}\n"
        f"ROLE  : {node_roles[current]}\n"
        f"BAT   : {node_battery[current]}%\n"
        f"SNR   : {node_snr[current]} dB\n"
        f"LOAD  : {node_load[current]} msgs\n"
        f"\n"
        f"CONG  : {cong['name']} (drop {int(DROP_TABLE[current_congestion][state['priority']]*100)}%)\n"
        f"TOTAL : {state['total']}\n"
        f"DLVRD : {state['delivered']}\n"
        f"DROP  : {state['dropped']}\n"
        f"AVG   : {avg:.1f} hops\n"
    )
    info_txt.set_color(PRIORITIES[prio]["color"])

    log_txt.set_text("\n".join(state["log"]))
    stats_txt.set_text(
        f"Nodes: {NUM_NODES}   Links: {G.number_of_edges()}   "
        f"Avg degree: {sum(d for _,d in G.degree())/NUM_NODES:.1f}   "
        f"Diameter: {nx.diameter(G)}"
    )

ani = animation.FuncAnimation(fig, animate, interval=500, cache_frame_data=False)
plt.show()
