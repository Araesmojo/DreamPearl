// Transform the vertex into eye space. 	
v_Position = vec3(u_MVMatrix * a_Position);            

// Transform the normal's orientation into eye space.
v_Normal = vec3(u_MVMatrix * vec4(a_Normal, 0.0));