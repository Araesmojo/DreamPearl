// Will be used for attenuation.
float distance = length(u_LightPos - v_Position);

// Get a lighting direction vector from the light to the vertex.
vec3 lightVector = normalize(u_LightPos - v_Position);              	

// Calculate the dot product of the light vector and vertex normal. If the normal and light vector are
// pointing in the same direction then it will get max illumination.
float diffuse = max(dot(v_Normal, lightVector), 0.0);               	  		  													  

// Add attenuation. 
diffuse = diffuse * (1.0 / (1.0 + (0.10 * distance)));
    
// Add ambient lighting
diffuse = diffuse + 0.3;