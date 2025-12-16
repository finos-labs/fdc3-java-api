#!/usr/bin/env node
/**
 * Script to generate NullHandlingMixin.java for FDC3 schema types.
 * 
 * This script reads JSON schema files and identifies fields that are:
 * - NOT required (optional)
 * - Do NOT allow null as a type
 * 
 * For these fields, we need @JsonInclude(Include.NON_NULL) to omit them
 * when null, otherwise JSON Schema validation fails.
 * 
 * Usage: node generate-mixins.js [schema-dir] [output-dir]
 */

const fs = require('fs');
const path = require('path');

// Configuration
const SCHEMA_DIR = process.argv[2] || path.join(__dirname, '../target/npm-work/node_modules/@finos/fdc3-schema/dist/schemas/api');
const OUTPUT_DIR = process.argv[3] || path.join(__dirname, '../src/main/java/org/finos/fdc3/schema');
const PACKAGE_NAME = 'org.finos.fdc3.schema';

// Track all mixins we need to generate
const mixinsNeeded = new Map(); // className -> Set of field names

/**
 * Check if a schema type allows null
 */
function allowsNull(schema) {
    if (!schema) return false;
    
    // Check for explicit null type
    if (schema.type === 'null') return true;
    
    // Check for array of types including null
    if (Array.isArray(schema.type) && schema.type.includes('null')) return true;
    
    // Check for oneOf/anyOf with null
    if (schema.oneOf) {
        return schema.oneOf.some(s => s.type === 'null');
    }
    if (schema.anyOf) {
        return schema.anyOf.some(s => s.type === 'null');
    }
    
    return false;
}

/**
 * Convert schema name to Java class name
 */
function toJavaClassName(name) {
    return name.charAt(0).toUpperCase() + name.slice(1);
}

/**
 * Convert property name to Java getter method name
 */
function toGetterName(propName) {
    return 'get' + propName.charAt(0).toUpperCase() + propName.slice(1);
}

/**
 * Get the Java type for a schema property (simplified)
 */
function getJavaType(schema, propName) {
    if (!schema) return 'Object';
    
    // Handle $ref - just use Object for simplicity
    if (schema.$ref) return 'Object';
    
    const type = schema.type;
    if (type === 'string') return 'String';
    if (type === 'integer') return 'Long';
    if (type === 'number') return 'Double';
    if (type === 'boolean') return 'Boolean';
    if (type === 'array') return 'Object';
    if (type === 'object') return 'Object';
    
    return 'Object';
}

/**
 * Process a schema definition and find optional non-nullable fields
 */
function processDefinition(name, schema, schemaFile) {
    if (!schema || schema.type !== 'object' || !schema.properties) {
        return;
    }
    
    const required = new Set(schema.required || []);
    const optionalNonNullFields = [];
    
    for (const [propName, propSchema] of Object.entries(schema.properties)) {
        // Skip if required
        if (required.has(propName)) continue;
        
        // Skip if allows null
        if (allowsNull(propSchema)) continue;
        
        // Skip if it's just "true" (any type allowed)
        if (propSchema === true) continue;
        
        // This field needs NON_NULL
        optionalNonNullFields.push({
            name: propName,
            type: getJavaType(propSchema, propName),
            jsonProperty: propName
        });
    }
    
    if (optionalNonNullFields.length > 0) {
        const className = toJavaClassName(name);
        if (!mixinsNeeded.has(className)) {
            mixinsNeeded.set(className, []);
        }
        mixinsNeeded.get(className).push(...optionalNonNullFields);
    }
}

/**
 * Process a schema file
 */
function processSchemaFile(filePath) {
    try {
        const content = fs.readFileSync(filePath, 'utf8');
        const schema = JSON.parse(content);
        
        // Process $defs
        if (schema.$defs) {
            for (const [name, def] of Object.entries(schema.$defs)) {
                processDefinition(name, def, filePath);
            }
        }
        
        // Process definitions (older style)
        if (schema.definitions) {
            for (const [name, def] of Object.entries(schema.definitions)) {
                processDefinition(name, def, filePath);
            }
        }
    } catch (e) {
        console.error(`Error processing ${filePath}: ${e.message}`);
    }
}

/**
 * Generate a single inner mixin class
 */
function generateInnerMixin(className, fields) {
    // Deduplicate fields by name
    const uniqueFields = new Map();
    for (const field of fields) {
        uniqueFields.set(field.name, field);
    }
    
    const fieldDefs = Array.from(uniqueFields.values()).map(field => {
        return `        @JsonProperty("${field.jsonProperty}")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        abstract ${field.type} ${toGetterName(field.name)}();`;
    }).join('\n\n');
    
    return `    public static abstract class ${className}Mixin {
${fieldDefs}
    }`;
}

/**
 * Generate the complete NullHandlingMixin.java file
 */
function generateNullHandlingMixin() {
    const sortedClassNames = Array.from(mixinsNeeded.keys()).sort();
    
    // Generate registration calls
    const registrations = sortedClassNames
        .map(className => `        om.addMixIn(${className}.class, ${className}Mixin.class);`)
        .join('\n');
    
    // Generate inner mixin classes
    const innerClasses = sortedClassNames
        .map(className => generateInnerMixin(className, mixinsNeeded.get(className)))
        .join('\n\n');
    
    return `package ${PACKAGE_NAME};

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Jackson Mix-ins for FDC3 schema types to handle optional fields.
 * 
 * GENERATED FILE - Do not edit manually!
 * Regenerate with: node scripts/generate-mixins.js
 * 
 * In JSON Schema, optional fields (not in "required" array) that don't allow null
 * should be omitted when null, not serialized as "field": null.
 * These mix-ins apply @JsonInclude(NON_NULL) to those fields.
 */
public final class NullHandlingMixin {
    
    private NullHandlingMixin() {
        // Utility class
    }
    
    /**
     * Register all mix-ins with the given ObjectMapper.
     */
    public static void registerAll(ObjectMapper om) {
${registrations}
    }
    
    // === Mix-in classes for each schema type ===
    
${innerClasses}
}
`;
}

// Main execution
console.log('Scanning schema files in:', SCHEMA_DIR);

// Check if schema directory exists
if (!fs.existsSync(SCHEMA_DIR)) {
    console.error(`Schema directory not found: ${SCHEMA_DIR}`);
    console.error('Run "mvn generate-sources" first to download schemas.');
    process.exit(1);
}

// Read all schema files
const schemaFiles = fs.readdirSync(SCHEMA_DIR)
    .filter(f => f.endsWith('.schema.json'))
    .map(f => path.join(SCHEMA_DIR, f));

// Process api.schema.json which has shared definitions
const apiSchemaPath = path.join(SCHEMA_DIR, 'api.schema.json');
if (fs.existsSync(apiSchemaPath)) {
    processSchemaFile(apiSchemaPath);
}

// Process each schema file
for (const file of schemaFiles) {
    processSchemaFile(file);
}

console.log(`\nFound ${mixinsNeeded.size} classes needing mixins:`);
for (const [className, fields] of mixinsNeeded) {
    // Deduplicate
    const uniqueFields = new Set(fields.map(f => f.name));
    console.log(`  - ${className} (${uniqueFields.size} fields)`);
}

// Generate the single NullHandlingMixin.java file
const outputPath = path.join(OUTPUT_DIR, 'NullHandlingMixin.java');
const content = generateNullHandlingMixin();

fs.writeFileSync(outputPath, content);
console.log(`\nGenerated: ${outputPath}`);

console.log('\nDone!');
