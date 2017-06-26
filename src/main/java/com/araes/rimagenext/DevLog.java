// List of near term goals
// - Multiple layer display
// - Single player character on ground
// - Collidables
// - Mobiles
// - Sky
// - Lighting
// - Fractal layers
//   - Terrain tiles extend well beyond player
//   - Larger and larger resolution tiles beyond sight
//   - When near player, break to find local values
//   - Colors are reference values rather than direct colors
//   - Color x, means 4 tiles below this are a,b,c,d
//   - Store 256 * 256 * 256 tiles as color references
//   - Try to keep colors close to average of source tile colors
//   - Can then draw realistic maps with higher level tiles
// - Sky with CFD
//   - Sky with multiple resolution layers
//   - Structure fractally expands out from player
//   - Follow volumes of substance (w. vapor, air)
//   - Flow pushes, diffuses, ect. water
//   - Input texture of local flow effects
//     - Stuff player can see
