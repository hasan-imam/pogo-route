# User Guide

## Designing route
### Bundles

Bundles (a.k.a. sets) are combinations of quests that a player enqueues before taking action to complete all of them together. Route planning creates these bundles as the output. Before planning, you get to choose the kind of bundles you want in your route. You define this by giving the program bundle patterns.

### Bundle pattern options
#### N of a kind
When you want to have the same quest in each bundle/set, use this type.

Example input: `3 x ['3B10', '3G15', 'DT15']`

Example output: Generated route will have bundles where each bundle will have 3 of `3B10` or `3G15` or `DT15` quests. This mechanism helps when the quest action is unique (e.g. catching a Ditto) and it only makes sense to have a homogenized set where a single action completes all quests in the set.

<details><summary>See sample generated route</summary>

```
40.571335,-74.145239, 3B10 PT2M
40.598808,-74.129353, 3B10 PT2M
40.60931,-74.118026, 3B10 PT2M (set 1)

40.634684,-74.166427, DT15 PT6M
40.607324,-74.057417, DT15 PT6M
40.621451,-73.971864, DT15 PT2M (set 2)

40.630949,-73.996572, 3B10 PT2M
40.645129,-74.002646, 3B10 PT2M
40.657339,-73.989915, 3B10 PT30S (set 3)

40.703731,-74.01102, 3G15 PT2M
40.732493,-73.997126, 3G15 PT2M
40.716283,-73.959309, 3G15 PT2M (set 4)

40.722093,-73.991093, DT15 PT2M
40.72789,-74.003244, DT15 PT2M
40.765601,-73.994143, DT15 PT0S (set 5)

40.765005,-73.99046, 3B10 PT30S
40.758989,-73.991514, 3B10 PT2M
40.756378,-73.978631, 3B10 PT30S (set 6)
```
</details>

#### Any of a set of things
Use this type when each bundle/set can have any quest from a set of possibilities.

Example input: `3 x ['3G10', '3G15']`

Example output: Each bundle in generate route will have 3 quests, comprised of `3G10`, `3G15` or a mix of both. `3G10` and `3G15` has almost the same action and both gives stardust as reward. Players are okay with having these quests together in a single bundle, so you can choose this bundle type to creates mixed sets.

<details><summary>See sample generated route</summary>

```
40.668649,-73.953532, 3G10 PT2M
40.668364,-73.92164, 3G15 PT2M
40.689311,-73.904376, 3G10 PT2M (set 1)

40.704091,-73.919598, 3G10 PT2M
40.710554,-73.933718, 3G10 PT2M
40.692519,-73.940065, 3G10 PT2M (set 2)

40.696372,-73.964158, 3G10 PT2M
40.716283,-73.959309, 3G15 PT2M
40.71823,-73.998111, 3G10 PT30S (set 3)
```
</details>

#### Ordered set of things
Each bundle will contain elements of type *and* order that you define.

Example input: `['DRC', 'DRC', 'DRT']`

Example output: Each bundle will contain a `DRC`, then a `DRC` and finally a `DRT`.

<details><summary>See sample generated route</summary>

```
40.590965,-73.961032, DRC PT2M
40.609023,-73.97494, DRC PT2M
40.642079,-73.995674, DRT PT6M (set 1)

40.686821,-74.017191, DRC PT2M
40.714161,-74.012372, DRC PT2M
40.719058,-73.986203, DRT PT2M (set 2)
```
</details>

#### Unordered set of things
Each bundle will contain one element of each type you mention, ignoring the order in which you define them. This is similar to 'Ordered set of things' above, except that this pattern doesn't care about the order in which you define the bundle elements.

Example input: `['DRC', 'DRC', 'DRT']`

Example output: Each bundle will contain 2 `DRC` and one `DRT`, in no specific order.

<details><summary>See sample generated route</summary>

```
40.853546,-73.877817, DRT PT2M
40.835348,-73.86208, DRC PT2M
40.827694,-73.869354, DRC PT30S (set 1)

40.831688,-73.874787, DRT PT6M
40.760479,-73.927054, DRC PT2M
40.75705,-73.966158, DRC PT0S (set 2)
```
</details>

### Defining a bundle

After viewing some details about the available quests, the program gives you option to define one or more bundles. To define each bundle, you one of the above bundle types and then give the actual pattern input.

#### Using multiple bundle patterns together
Note that if you define multiple patterns in one go, the generated route will have bundles matching any of your defined types. How's that helpful? Say you define the Dratini/rare candy bundles using unordered set (`['DRC', 'DRC', 'DRT']`). And then also put an N of a kind pattern `3 x ['DTC']`. The bundles in the generated route will match either one of these patterns.

<details><summary>See sample generated route</summary>

```
40.853546,-73.877817, DRT PT2M
40.835348,-73.86208, DRC PT2M
40.827694,-73.869354, DRC PT30S (set 1)

40.831688,-73.874787, DRT PT6M
40.760479,-73.927054, DRC PT2M
40.75705,-73.966158, DRC PT0S (set 2)

40.757238,-73.964098, DRT PT2M
40.752485,-73.980395, DRC PT2M
40.73679,-74.009185, DRC PT2M (set 3)

40.722244,-74.000376, DTC PT2M
40.75728,-73.970164, DTC PT6M
40.806546,-73.966558, DTC PT30S (set 4)

40.804195,-73.961098, DRT PT11M
40.714161,-74.012372, DRC PT2M
40.686821,-74.017191, DRC PT2M (set 5)

40.693973,-73.993796, DRT PT6M
40.609023,-73.97494, DRC PT2M
40.590965,-73.961032, DRC PT2M (set 6)

40.599317,-73.966152, DTC PT2M
40.632019,-73.948604, DTC PT6M
40.682077,-73.851626, DTC PT6M (set 7)

40.752392,-73.853444, DTC PT2M
40.750369,-73.894059, DTC PT30S
40.758037,-73.900083, DTC END (set 8)
```
</details>

#### Ways you can define bundle elements

You have the following options:
 1. Using abbreviation. Example input: `3 x ['DTC']`
 2. Using action and reward descriptions. You enter action, space, hyphen, space, then reward. Example input: `action - reward` Example input: `3 x ['Catch 5 weather boosted Pokémon - 3 Razz Berries', 'Power up a Pokémon 10 times - 3 Silver Pinap Berries']`. 

Note that...
 - You need to put each bundle element (such as `Catch 5 weather boosted Pokémon - 3 Razz Berries`) inside single or double quote (`'` or `"`).
 - If abbreviation exists for a quest, you must define that quest using abbreviation. Example wrong input: `3 x ["Win 3 gym Battles - 1000 Stardust", "Catch 5 weather boosted Pokémon - 3 Razz Berries"]`. Corrected input: `3 x ["3B10", "Catch 5 weather boosted Pokémon - 3 Razz Berries"]`

### Max cost
The program gives you the option to define a 'maximum cost' for the route. If you define it, the program generates a route that doesn't exceed that cost.

Basically, the routing program is minimizing cost of visiting all points that matches your intent. Costs can be defined as...
 - the distance one would travel if one visited all of those points
 - the time one would spend in cooldown if they visited all the points

Currently the program minimizes the distance cost, so your max cost input needs to be in terms of distance. If you define 100 km as max cost, generated route will be less than or equal to 100 km in traveling distance.

If you want to define this limit in terms of duration (e.g. 2 hrs), please let the dev team know. Although making that change *may* generate sub-optimal route.

## Storing quest data

Every time the program fetches data from map websites, it stores the fetched data locally. At most 3 such files are stored for each map. All subsequent storing action also deletes the oldest file to keep the file count within limit.

Be sure to always run the program from the same directory. As you run the program, you will notice that a folder names 'quests' has been created. This is where these files are stored. Be sure not to put any other files in these directories.

After you select a map, if the program finds old data from such stored file, it will give you the option to re-use the latest data. Ideally you'd get real data once per day and re-use it for all subsequent runs for that day. You may also want to refetch real data if you think more quests have been added since you last fetched.
